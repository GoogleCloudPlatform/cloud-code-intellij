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

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

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
          callback.succeeded(new AppEngineDeploymentRuntimeImpl(
              project, appEngineHelper, getLoggingHandler(), deploymentOutput.toString()));
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
}
