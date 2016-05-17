/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.app.api.AppEngineException;
import com.google.cloud.tools.app.impl.cloudsdk.CloudSdkAppEngineDeployment;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessOutputLineListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.sdk.CloudSdk;
import com.google.cloud.tools.app.impl.config.DefaultDeployConfiguration;
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * Performs the deployment of App Engine based applications to GCP.
 */
public class AppEngineDeployAction extends AppEngineAction {

  private static final Logger logger = Logger.getInstance(AppEngineDeployAction.class);

  private Project project;
  private File deploymentArtifactPath;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private AppEngineHelper appEngineHelper;
  private DeploymentOperationCallback callback;
  private DeploymentArtifactType artifactType;

  /**
   * Initialize the deployment action.
   */
  public AppEngineDeployAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Project project,
      @NotNull File deploymentArtifactPath,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull DeploymentOperationCallback callback) {
    super(loggingHandler, appEngineHelper, deploymentConfiguration);

    this.appEngineHelper = appEngineHelper;
    this.project = project;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.deploymentConfiguration = deploymentConfiguration;
    this.callback = callback;
    this.artifactType = DeploymentArtifactType.typeForPath(deploymentArtifactPath);
  }

  @Override
  public void run() {
    File stagingDirectory = stage();

    if (stagingDirectory != null) {
      deploy(stagingDirectory);
    }
  }

  /**
   * Stage the deployment artifacts and return the staging directory or null if it failed.
   */
  @Nullable
  private File stage() {
    File stagingDirectory;
    try {
      stagingDirectory = FileUtil.createTempDirectory(
          "gae-mvm" /* prefix */,
          null /* suffix */,
          true  /* deleteOnExit */);
      consoleLogLn(
          "Created temporary staging directory: " + stagingDirectory.getAbsolutePath());
    } catch (IOException ex) {
      logger.warn(ex);
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.error.creating.staging.directory"));
      return null;
    }

    try {
      File stagedArtifactPath =
          copyFile(stagingDirectory, "target" + artifactType, deploymentArtifactPath);
      stagedArtifactPath.setReadable(true /* readable */, false /* ownerOnly */);

      File appYamlPath = deploymentConfiguration.isAuto()
          ? appEngineHelper.defaultAppYaml()
          : CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getAppYamlPath());

      File dockerFilePath = deploymentConfiguration.isAuto()
          ? appEngineHelper.defaultDockerfile(
          DeploymentArtifactType.typeForPath(deploymentArtifactPath))
          : CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getDockerFilePath());

      copyFile(stagingDirectory, "app.yaml", appYamlPath);
      copyFile(stagingDirectory, "Dockerfile", dockerFilePath);
    } catch (IOException ex) {
      logger.warn(ex);
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.during.staging"));
      return null;
    }

    return stagingDirectory;
  }

  /**
   * Perform a deployment from the given staging directory.
   */
  private void deploy(@NotNull File stagingDirectory) {
    final StringBuilder rawDeployOutput = new StringBuilder();
    CloudSdk sdk;

    ProcessOutputLineListener stdErrListener = new ProcessOutputLineListener() {
      @Override
      public void outputLine(String output) {
        consoleLogLn(output);
      }
    };

    ProcessOutputLineListener stdOutListener = new ProcessOutputLineListener() {
      @Override
      public void outputLine(String output) {
        rawDeployOutput.append(output);
      }
    };

    ProcessExitListener deployExitListener = new DeployExitListener(rawDeployOutput);

    try {
      sdk = prepareExecution(stdErrListener, stdOutListener, deployExitListener);
    } catch (AppEngineException ex) {
      callback.errorOccurred(GctBundle.message("appengine.deployment.error"));
      return;
    }

    DefaultDeployConfiguration configuration = new DefaultDeployConfiguration();
    configuration.setDeployables(Collections.singletonList(new File(stagingDirectory, "app.yaml")));
    configuration.setProject(deploymentConfiguration.getCloudProjectName());
    configuration.setPromote(true);
    if (!StringUtil.isEmpty(deploymentConfiguration.getVersion())) {
      configuration.setVersion(deploymentConfiguration.getVersion());
    }

    CloudSdkAppEngineDeployment deployment = new CloudSdkAppEngineDeployment(sdk);
    deployment.deploy(configuration);
  }

  private class DeployExitListener implements ProcessExitListener {
    final StringBuilder rawDeployOutput;

    DeployExitListener(StringBuilder rawDeployOutput) {
      this.rawDeployOutput = rawDeployOutput;
    }

    @Override
    public void exit(int exitCode) {
      try {
        if (exitCode == 0) {
          DeployOutput deployOutput = null;

          try {
            deployOutput = parseDeployOutput(rawDeployOutput.toString());
          } catch (JsonParseException ex) {
            logger.error("Could not retrieve service/version info of deployed application", ex);
          }

          if (deployOutput == null
              || deployOutput.getService() == null || deployOutput.getVersion() == null) {
            consoleLogLn(
                GctBundle.message("appengine.deployment.version.extract.failure") + "\n");
          }

          callback.succeeded(
              new AppEngineDeploymentRuntime(
                  project, appEngineHelper, getLoggingHandler(), deploymentConfiguration,
                  deployOutput != null ? deployOutput.getService() : null,
                  deployOutput != null ? deployOutput.getVersion() : null));
        } else if (cancelled) {
          callback.errorOccurred(GctBundle.message("appengine.deployment.error.cancelled"));
        } else {
          logger.warn("Deployment process exited with an error. Exit Code:" + exitCode);
          callback.errorOccurred(
              GctBundle.message("appengine.deployment.error.with.code", exitCode));
        }
      } finally {
        deleteCredentials();
      }
    }
  }

  private File copyFile(File stagingDirectory, String targetFileName, File sourceFilePath)
      throws IOException {
    File destinationFilePath = new File(stagingDirectory, targetFileName);
    FileUtil.copy(sourceFilePath, destinationFilePath);
    consoleLogLn("Copied %s %s to %s", targetFileName,
        sourceFilePath.getAbsolutePath(), destinationFilePath.getAbsolutePath());
    return destinationFilePath;
  }

  /**
   * Parse the raw json output of the deployment.
   *
   * @return an object modeling the output of a deploy command
   * @throws JsonParseException if unable to extract the deploy output information needed
   */
  @VisibleForTesting
  static DeployOutput parseDeployOutput(String jsonOutput) throws JsonParseException {
    Type deployOutputType = new TypeToken<DeployOutput>() {}.getType();
    DeployOutput deployOutput = new Gson().fromJson(jsonOutput, deployOutputType);
    if (deployOutput == null
        || deployOutput.versions == null || deployOutput.versions.size() != 1) {
      throw new JsonParseException("Cannot get app version: unexpected gcloud JSON output format");
    }
    return deployOutput;
  }

  /**
   * Holds de-serialized JSON output of gcloud app deploy. Don't change the field names
   * because Gson uses it for automatic de-serialization.
   */
  @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Initialized by Gson")
  static class DeployOutput {
    private static class Version {
      String id;
      String service;
    }

    List<Version> versions;

    @Nullable
    public String getVersion() {
      if (versions == null || versions.size() != 1) {
        return null;
      }
      return versions.get(0).id;
    }

    @Nullable
    public String getService() {
      if (versions == null || versions.size() != 1) {
        return null;
      }
      return versions.get(0).service;
    }
  }
}
