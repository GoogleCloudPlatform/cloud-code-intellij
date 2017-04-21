/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineExecutor;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineFlexibleDeployTask;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineStandardDeployTask;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleStage;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardStage;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.Deployment;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * A Cloud SDK (gcloud) based implementation of the {@link AppEngineHelper} interface.
 */
public class CloudSdkAppEngineHelper implements AppEngineHelper {

  private static final Logger logger = Logger.getInstance(CloudSdkAppEngineHelper.class);

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";
  public static final String APP_ENGINE_BILLING_URL = "https://cloud.google.com/appengine/pricing";

  private final Project project;
  private Path credentialsPath;

  /**
   * Initialize the helper.
   */
  public CloudSdkAppEngineHelper(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public Optional<CancellableRunnable> createDeployRunner(
      LoggingHandler loggingHandler,
      DeploymentSource source,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      DeploymentOperationCallback callback) {

    if (!(source instanceof AppEngineDeployable)) {
      callback.errorOccurred(GctBundle.message("appengine.deployment.invalid.source.error"));
      throw new RuntimeException("Invalid deployment source selected for deployment");
    }

    if (CloudSdkService.getInstance().validateCloudSdk().contains(
        CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND)) {
      callback.errorOccurred(GctBundle.message("appengine.cloudsdk.location.invalid.message") + " "
          + CloudSdkService.getInstance().getSdkHomePath());
      return Optional.empty();
    }

    if (source.getFile() == null || !source.getFile().exists()) {
      callback.errorOccurred(GctBundle.message("appengine.deployment.source.not.found.error",
          source.getFilePath()));
      return Optional.empty();
    }

    AppEngineEnvironment targetEnvironment = ((AppEngineDeployable) source).getEnvironment();

    AppEngineDeploy deploy = new AppEngineDeploy(
        this,
        loggingHandler,
        deploymentConfiguration,
        targetEnvironment,
        wrapCallbackForUsageTracking(callback, targetEnvironment));

    if (targetEnvironment.isStandard() || targetEnvironment.isFlexCompat()) {
      // We need this fresh check in case appengine-web.xml gets modified to turn compat<->standard,
      // or vice-versa, and that change gets picked up.
      boolean isFlexCompat = AppEngineProjectService.getInstance().isFlexCompat(project, source);

      return Optional.of(createStandardRunner(loggingHandler, Paths.get(source.getFilePath()),
          deploy, isFlexCompat));
    } else if (targetEnvironment.isFlexible()) {
      try {
        // Checks if the Yaml or Dockerfile exist.
        Optional<FlexibleRuntime> runtimeOptional =
            AppEngineProjectService.getInstance().getFlexibleRuntimeFromAppYaml(
                deploymentConfiguration.getAppYamlPath());

        if (!Files.exists(Paths.get(deploymentConfiguration.getAppYamlPath()))) {
          callback.errorOccurred(GctBundle.getString("appengine.deployment.error.staging.yaml"));
          return Optional.empty();
        }
        if (runtimeOptional.filter(runtime -> runtime == FlexibleRuntime.CUSTOM).isPresent()
            && !Files.exists(Paths.get(deploymentConfiguration.getDockerFilePath()))) {
          callback.errorOccurred(
              GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
          return Optional.empty();
        }
        return Optional.of(createFlexRunner(loggingHandler, Paths.get(source.getFilePath()),
            deploymentConfiguration, deploy));
      } catch (MalformedYamlFileException myf) {
        callback.errorOccurred(
            GctBundle.message("appengine.appyaml.malformed") + "\n" + myf.getMessage());
        return Optional.empty();
      }
    } else {
      throw new AssertionError("Invalid App Engine target environment: " + targetEnvironment);
    }
  }

  private AppEngineExecutor createStandardRunner(
      LoggingHandler loggingHandler,
      Path artifactToDeploy,
      AppEngineDeploy deploy,
      boolean isFlexCompat) {
    AppEngineStandardStage standardStage = new AppEngineStandardStage(
          this,
          loggingHandler,
          artifactToDeploy);

    return new AppEngineExecutor(
        new AppEngineStandardDeployTask(deploy, standardStage, isFlexCompat));
  }

  private AppEngineExecutor createFlexRunner(
      LoggingHandler loggingHandler,
      Path artifactToDeploy,
      AppEngineDeploymentConfiguration config,
      AppEngineDeploy deploy) {
    return new AppEngineExecutor(
        new AppEngineFlexibleDeployTask(deploy,
            new AppEngineFlexibleStage(loggingHandler, artifactToDeploy, config)));
  }

  @Override
  public Path createStagingDirectory(
      LoggingHandler loggingHandler,
      String cloudProjectName) throws IOException {
    Path stagingDirectory = FileUtil.createTempDirectory(
        "gae-staging-" + cloudProjectName /* prefix */,
        null /* suffix */,
        true /* deleteOnExit */).toPath();
    loggingHandler.print(
        "Created temporary staging directory: " + stagingDirectory.toString() + "\n");

    return stagingDirectory;
  }

  @Override
  public CloudSdk createSdk(
      LoggingHandler loggingHandler,
      ProcessStartListener startListener,
      ProcessOutputLineListener logListener,
      ProcessOutputLineListener outputListener,
      ProcessExitListener exitListener) {
    if (credentialsPath == null) {
      loggingHandler.print(GctBundle.message("appengine.action.credential.not.found") + "\n");
      throw new AppEngineException("Failed to create application default credentials.");
    }

    CloudToolsPluginInfoService pluginInfoService =
        ServiceManager.getService(CloudToolsPluginInfoService.class);

    return new CloudSdk.Builder()
        .sdkPath(CloudSdkService.getInstance().getSdkHomePath())
        .async(true)
        .addStdErrLineListener(logListener)
        .addStdOutLineListener(outputListener)
        .exitListener(exitListener)
        .startListener(startListener)
        .appCommandCredentialFile(credentialsPath.toFile())
        .appCommandMetricsEnvironment(pluginInfoService.getExternalPluginName())
        .appCommandMetricsEnvironmentVersion(pluginInfoService.getPluginVersion())
        .appCommandOutputFormat("json")
        .build();
  }

  @Override
  public Optional<Path> stageCredentials(String googleUserName) {
    if (Services.getLoginService().ensureLoggedIn(googleUserName)) {
      return doStageCredentials(googleUserName);
    }

    return Optional.empty();
  }

  private Optional<Path> doStageCredentials(String googleUsername) {
    Optional<CredentialedUser> projectUser =
        Services.getLoginService().getLoggedInUser(googleUsername);

    GoogleLoginState googleLoginState;
    if (projectUser.isPresent()) {
      googleLoginState = projectUser.get().getGoogleLoginState();
    } else {
      return Optional.empty();
    }

    String clientId = googleLoginState.fetchOAuth2ClientId();
    String clientSecret = googleLoginState.fetchOAuth2ClientSecret();
    String refreshToken = googleLoginState.fetchOAuth2RefreshToken();
    Map<String, String> credentialMap =
        ImmutableMap.of(
            CLIENT_ID_LABEL, clientId,
            CLIENT_SECRET_LABEL, clientSecret,
            REFRESH_TOKEN_LABEL, refreshToken,
            GCLOUD_USER_TYPE_LABEL, GCLOUD_USER_TYPE);
    String jsonCredential = new Gson().toJson(credentialMap);
    try {
      credentialsPath =
          FileUtil.createTempFile(
                  "tmp_google_application_default_credential", "json", true /* deleteOnExit */)
              .toPath();
      Files.write(credentialsPath, jsonCredential.getBytes(Charsets.UTF_8));

      return Optional.of(credentialsPath);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void deleteCredentials() {
    if (credentialsPath != null && Files.exists(credentialsPath)) {
      try {
        Files.delete(credentialsPath);
      } catch (IOException ioe) {
        logger.warn("failed to delete credential file expected at " + credentialsPath);
      }
    }
  }

  @NotNull
  private DeploymentOperationCallback wrapCallbackForUsageTracking(
      final DeploymentOperationCallback deploymentCallback,
      AppEngineEnvironment environment) {

    StringBuilder labelBuilder = new StringBuilder();
    if (environment == AppEngineEnvironment.APP_ENGINE_STANDARD) {
      labelBuilder.append("standard");
    } else {
      labelBuilder.append("flex");
    }
    final String eventLabel = labelBuilder.toString();

    return new DeploymentOperationCallback() {
      @Override
      public Deployment succeeded(@NotNull DeploymentRuntime deploymentRuntime) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_DEPLOY_SUCCESS)
            .addMetadata(GctTracking.METADATA_LABEL_KEY, eventLabel)
            .ping();
        return deploymentCallback.succeeded(deploymentRuntime);
      }

      @Override
      public void errorOccurred(@NotNull String errorMessage) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_DEPLOY_FAIL)
            .addMetadata(GctTracking.METADATA_LABEL_KEY, eventLabel)
            .addMetadata(GctTracking.METADATA_MESSAGE_KEY, errorMessage)
            .ping();
        deploymentCallback.errorOccurred(errorMessage);
      }
    };
  }

  @NotNull
  private Path getFileFromResourcePath(String resourcePath) {
    Path appYaml;
    try {
      URL resource = this.getClass().getClassLoader().getResource(resourcePath);
      Preconditions
          .checkArgument(resource != null, resourcePath + " is not a valid resource path.");
      appYaml = Paths.get(resource.toURI());
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    return appYaml;
  }

  @VisibleForTesting
  Path getCredentialsPath() {
    return credentialsPath;
  }
}
