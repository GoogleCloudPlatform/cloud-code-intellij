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
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Performs the deployment of App Engine based applications to GCP.
 */
class AppEngineDeployAction extends AppEngineAction {

  private static final Logger logger = Logger.getInstance(AppEngineDeployAction.class);

  private Project project;
  private File deploymentArtifactPath;
  private File appYamlPath;
  private File dockerFilePath;
  private String version;
  private AppEngineHelper appEngineHelper;
  private DeploymentOperationCallback callback;
  private DeploymentArtifactType artifactType;

  private static final String STOP_CONFIRMATION_URI_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/docs/java/console/#versions'>";
  private static final String STOP_CONFIRMATION_URI_CLOSE_TAG = "</a>";

  AppEngineDeployAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Project project,
      @NotNull File deploymentArtifactPath,
      @NotNull File appYamlPath,
      @NotNull File dockerFilePath,
      @Nullable String version,
      @NotNull DeploymentOperationCallback callback) {
    super(loggingHandler, appEngineHelper, callback);

    this.appEngineHelper = appEngineHelper;
    this.project = project;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.callback = callback;
    this.appYamlPath = appYamlPath;
    this.dockerFilePath = dockerFilePath;
    this.version = version;
    this.artifactType = DeploymentArtifactType.typeForPath(deploymentArtifactPath);
  }

  public void run() {
    File stagingDirectory;
    try {
      stagingDirectory = FileUtil.createTempDirectory(
          "gae-mvm" /* prefix */,
          null /* suffix */,
          true  /* deleteOnExit */);
      consoleLogLn(
          "Created temporary staging directory: " + stagingDirectory.getAbsolutePath());
    } catch (IOException e) {
      logger.warn(e);
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.error.creating.staging.directory"));
      return;
    }

    GeneralCommandLine commandLine = new GeneralCommandLine(
        appEngineHelper.getGcloudCommandPath().getAbsolutePath());
    commandLine.addParameters("preview", "app", "deploy", "--promote");
    commandLine.addParameter("app.yaml");
    if (!StringUtil.isEmpty(version)) {
      commandLine.addParameter("--version=" + version);
    }
    commandLine.addParameter("--format=json");

    commandLine.withWorkDirectory(stagingDirectory);
    consoleLogLn("Working directory set to: " + stagingDirectory.getAbsolutePath());

    try {
      File stagedArtifactPath =
          copyFile(stagingDirectory, "target" + artifactType, deploymentArtifactPath);
      stagedArtifactPath.setReadable(true /* readable */, false /* ownerOnly */);

      copyFile(stagingDirectory, "app.yaml", this.appYamlPath);
      copyFile(stagingDirectory, "Dockerfile", this.dockerFilePath);
    } catch (IOException e) {
      logger.warn(e);
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.during.staging"));
      return;
    }

    try {
      executeProcess(commandLine, new DeployToAppEngineProcessListener());
    } catch (ExecutionException e) {
      logger.warn(e);
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.during.execution"));
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

  private class DeployToAppEngineProcessListener extends ProcessAdapter {
    private StringBuilder deploymentOutput = new StringBuilder();

    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
      if(outputType.equals(ProcessOutputTypes.STDOUT)) {
        deploymentOutput.append(event.getText());
      }
    }

    @Override
    public void processTerminated(final ProcessEvent event) {
      try {
        if (event.getExitCode() == 0) {
          callback.succeeded(new DeploymentRuntimeImpl(deploymentOutput.toString()));
        } else if (cancelled) {
          callback.errorOccurred(GctBundle.message("appengine.deployment.error.cancelled"));
        } else {
          logger.warn("Deployment process exited with an error. Exit Code:" + event.getExitCode());
          callback.errorOccurred(
              GctBundle.message("appengine.deployment.error.with.code", event.getExitCode()));
        }
      } finally {
        deleteCredentials();
      }

    }
  }

  private class DeploymentRuntimeImpl extends DeploymentRuntime {
    private String deploymentOutput;

    public DeploymentRuntimeImpl(@NotNull String deploymentOutput) {
      this.deploymentOutput = deploymentOutput;
    }

    @Override
    public boolean isUndeploySupported() {
      return true;
    }

    @Override
    public void undeploy(@NotNull final UndeploymentTaskCallback callback) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          int doStop = Messages
              .showOkCancelDialog(
                  GctBundle.message(
                      "appengine.stop.modules.version.confirmation.message",
                      STOP_CONFIRMATION_URI_OPEN_TAG,
                      STOP_CONFIRMATION_URI_CLOSE_TAG),
                  GctBundle.message("appengine.stop.modules.version.confirmation.title"),
                  General.Warning);

          if (doStop == Messages.YES) {
            stop(callback);
          } else {
            callback.errorOccurred(
                GctBundle.message("appengine.stop.modules.version.canceled.message"));
          }
        }
      });
    }

    private void stop(@NotNull UndeploymentTaskCallback callback) {
      DeployOutput deployOutput;
      try {
        deployOutput = parseDeployOutputToService(deploymentOutput);
      } catch (JsonParseException e) {
        logger.warn("Could not retrieve module(s) of deployed application", e);
        callback.errorOccurred(GctBundle.message("appengine.stop.modules.version.execution.error"));
        return;
      }

      final AppEngineAction appEngineStopAction = appEngineHelper.createStopAction(
          getLoggingHandler(),
          deployOutput.getService(),
          deployOutput.getVersion(),
          callback);

      ProgressManager.getInstance()
          .run(new Task.Backgroundable(project, "Stop App Engine", true,
              null) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              ApplicationManager.getApplication().invokeLater(appEngineStopAction);
            }
          });
    }
  }

  // Holds de-serialized JSON output of gcloud app deploy.
  @VisibleForTesting
  static class DeployOutput {
    private static class Version {
      String id;
      String service;
    }
    List<Version> versions;

    @NotNull
    public String getVersion() {
      if (versions.size() != 1) {
        throw new AssertionError("Should be called only when 'versions' have one element.");
      }
      return versions.get(0).id;
    }

    @NotNull
    public String getService() {
      if (versions.size() != 1) {
        throw new AssertionError("Should be called only when 'versions' have one element.");
      }
      return versions.get(0).service;
    }
  }

  @VisibleForTesting
  DeployOutput parseDeployOutputToService(String jsonOutput) throws JsonParseException {
    /* An example JSON output of gcloud app deloy:
        {
          "configs": [],
          "versions": [
            {
              "id": "20160429t112518",
              "last_deployed_time": null,
              "project": "springboot-maven-project",
              "service": "default",
              "traffic_split": null,
              "version": null
            }
          ]
        }
    */
    Type deployOutputType = new TypeToken<DeployOutput>() {}.getType();
    DeployOutput deployOutput = new Gson().fromJson(jsonOutput, deployOutputType);
    if (deployOutput == null || deployOutput.versions.size() != 1) {
      throw new JsonParseException("Cannot get app version: unexpected gcloud JSON output format");
    }
    return deployOutput;
  }
}
