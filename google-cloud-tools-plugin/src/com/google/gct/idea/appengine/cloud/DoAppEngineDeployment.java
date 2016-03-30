/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.Services;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Performs the deployment of App Engine based applications to GCP.
 */
class DoAppEngineDeployment implements Runnable {

  private static final Logger logger = Logger.getInstance(DoAppEngineDeployment.class);

  private LoggingHandler loggingHandler;
  private File deploymentArtifactPath;
  private File appYamlPath;
  private File dockerFilePath;
  private AppEngineHelper appEngineHelper;
  private DeploymentOperationCallback callback;
  private DeploymentArtifactType artifactType;

  DoAppEngineDeployment(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull File deploymentArtifactPath,
      @Nullable File appYamlPath,
      @Nullable File dockerFilePath,
      @NotNull DeploymentOperationCallback callback) {
    this.appEngineHelper = appEngineHelper;
    this.loggingHandler = loggingHandler;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.callback = callback;
    this.appYamlPath = appYamlPath;
    this.dockerFilePath = dockerFilePath;
    this.artifactType = DeploymentArtifactType.typeForPath(deploymentArtifactPath);
  }

  public void run() {
    final File appDefaultCredentialsPath = createApplicationDefaultCredentials();
    if (appDefaultCredentialsPath == null) {
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.credential.not.found",
              appEngineHelper.getGoogleUsername()));
      return;
    }
    File stagingDirectory;
    try {
      stagingDirectory = FileUtil.createTempDirectory(
          "gae-mvm" /* prefix */,
          null /* suffix */,
          true  /* deleteOnExit */);
      consoleLogLn(
          loggingHandler,
          "Created temporary staging directory: " + stagingDirectory.getAbsolutePath());
    } catch (IOException e) {
      logger.error(e);
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.error.creating.staging.directory"));
      return;
    }

    try {
      File stagedArtifactPath =
          copyFile(stagingDirectory, "target" + artifactType, deploymentArtifactPath);
      stagedArtifactPath.setReadable(true /* readable */, false /* ownerOnly */);

      if (this.appYamlPath != null) {
        copyFile(stagingDirectory, "app.yaml", this.appYamlPath);
      }
      if (this.dockerFilePath != null) {
        copyFile(stagingDirectory, "Dockerfile", this.dockerFilePath);
      }
    } catch (IOException e) {
      logger.error(e);
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.during.staging"));
      return;
    }

    GeneralCommandLine commandLine = new GeneralCommandLine(
        appEngineHelper.getGcloudCommandPath().getAbsolutePath());
    commandLine.addParameters("preview", "app", "deploy", "app.yaml", "--promote", "--quiet");
    commandLine.addParameter("--project=" + appEngineHelper.getProjectId());
    commandLine
        .addParameter("--credential-file-override=" + appDefaultCredentialsPath.getAbsolutePath());
    commandLine.withWorkDirectory(stagingDirectory);
    consoleLogLn(loggingHandler, "Working directory set to: " + stagingDirectory.getAbsolutePath());
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);
    commandLine.getEnvironment().put("CLOUDSDK_METRICS_ENVIRONMENT", "gcloud-intellij");
    commandLine.getEnvironment().put("CLOUDSDK_APP_USE_GSUTIL", "0");
    Process process = null;
    try {
      consoleLogLn(loggingHandler, "Executing: " + commandLine.getCommandLineString());
      process = commandLine.createProcess();
    } catch (ExecutionException e) {
      logger.error(e);
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.during.execution"));
      return;
    }
    final ProcessHandler processHandler = new OSProcessHandler(process,
        commandLine.getCommandLineString());
    loggingHandler.attachToProcess(processHandler);
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        if (event.getExitCode() == 0) {
          callback.succeeded(new DeploymentRuntime() {
            @Override
            public boolean isUndeploySupported() {
              return false;
            }

            @Override
            public void undeploy(@NotNull UndeploymentTaskCallback callback) {
              throw new UnsupportedOperationException();
            }
          });
        } else {
          logger.error("Deployment process exited with an error. Exit Code:" + event.getExitCode());
          callback.errorOccurred(
              GctBundle.message("appengine.deployment.error.with.code", event.getExitCode()));
        }
        if (appDefaultCredentialsPath.exists()) {
          if (!appDefaultCredentialsPath.delete()) {
            logger.warn("failed to delete credential file expected at "
                + appDefaultCredentialsPath.getPath());
          }
        }
      }
    });
    processHandler.startNotify();
  }

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";

  @VisibleForTesting
  protected File createApplicationDefaultCredentials() {
    CredentialedUser projectUser = Services.getLoginService().getAllUsers()
        .get(appEngineHelper.getGoogleUsername());

    GoogleLoginState googleLoginState = null;
    if (projectUser != null) {
      googleLoginState = projectUser
          .getGoogleLoginState();
    } else {
      return null;
    }
    String clientId = googleLoginState.fetchOAuth2ClientId();
    String clientSecret = googleLoginState.fetchOAuth2ClientSecret();
    String refreshToken = googleLoginState.fetchOAuth2RefreshToken();
    Map<String, String> credentialMap = ImmutableMap.of(
        CLIENT_ID_LABEL, clientId,
        CLIENT_SECRET_LABEL, clientSecret,
        REFRESH_TOKEN_LABEL, refreshToken,
        GCLOUD_USER_TYPE_LABEL, GCLOUD_USER_TYPE
    );
    String jsonCredential = new Gson().toJson(credentialMap);
    File tempCredentialFilePath = null;
    try {
      tempCredentialFilePath = FileUtil
          .createTempFile(
              "tmp_google_application_default_credential",
              "json",
              true /* deleteOnExit */);
      Files.write(jsonCredential, tempCredentialFilePath, Charset.forName("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return tempCredentialFilePath;
  }

  private File copyFile(File stagingDirectory, String targetFileName, File sourceFilePath)
      throws IOException {
    File destinationFilePath = new File(stagingDirectory, targetFileName);
    FileUtil.copy(sourceFilePath, destinationFilePath);
    consoleLogLn(loggingHandler, "Copied %s %s to %s", targetFileName,
        sourceFilePath.getAbsolutePath(), destinationFilePath.getAbsolutePath());
    return destinationFilePath;
  }

  private void consoleLogLn(LoggingHandler deploymentLoggingHandler, String message,
      String... arguments) {
    deploymentLoggingHandler.print(String.format(message + "\n", (Object[]) arguments));
  }
}
