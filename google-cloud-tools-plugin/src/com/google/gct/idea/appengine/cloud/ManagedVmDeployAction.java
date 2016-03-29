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

import com.google.gct.idea.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Performs the deployment of ManagedVM based applications to GCP.
 */
class ManagedVmDeployAction extends ManagedVmAction {
  private static final Logger logger = Logger.getInstance(ManagedVmDeployAction.class);

  private Project project;
  private File deploymentArtifactPath;
  private File appYamlPath;
  private File dockerFilePath;
  private AppEngineHelper appEngineHelper;
  private DeploymentArtifactType artifactType;
  private DeploymentOperationCallback callback;

  ManagedVmDeployAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Project project,
      @NotNull File deploymentArtifactPath,
      @NotNull File appYamlPath,
      @NotNull File dockerFilePath,
      @NotNull DeploymentOperationCallback callback) {
    super(loggingHandler, appEngineHelper);

    this.appEngineHelper = appEngineHelper;
    this.project = project;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.appYamlPath = appYamlPath;
    this.dockerFilePath = dockerFilePath;
    this.artifactType = DeploymentArtifactType.typeForPath(deploymentArtifactPath);
    this.callback = callback;
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

      copyFile(stagingDirectory, "app.yaml", this.appYamlPath);
      copyFile(stagingDirectory, "Dockerfile", this.dockerFilePath);
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
    consoleLogLn("Working directory set to: " + stagingDirectory.getAbsolutePath());
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);

    try {
      executeProcess(commandLine, new DeployToManagedVmProcessListener(appDefaultCredentialsPath));
    } catch (ExecutionException e) {
      logger.error(e);
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

  private class DeployToManagedVmProcessListener extends ProcessAdapter {
    private File defaultCredentialsPath;

    public DeployToManagedVmProcessListener(File defaultCredentialsPath) {
      this.defaultCredentialsPath = defaultCredentialsPath;
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      if (event.getExitCode() == 0) {
        callback.succeeded(new DeploymentRuntime() {
          @Override
          public boolean isUndeploySupported() {
            return true;
          }

          @Override
          public void undeploy(@NotNull UndeploymentTaskCallback callback) {
            final ManagedVmAction managedVmStopAction = appEngineHelper.createManagedVmStopAction(
                getLoggingHandler(),
                callback);

            ProgressManager.getInstance()
                .run(new Task.Backgroundable(project, "Stopping MVM", true,
                    null) {
                  @Override
                  public void run(@NotNull ProgressIndicator indicator) {
                    ApplicationManager.getApplication().invokeLater(managedVmStopAction);
                  }
                });
          }
        });
      } else {
        logger.error("Deployment process exited with an error. Exit Code:" + event.getExitCode());
        callback.errorOccurred(
            GctBundle.message("appengine.deployment.error.with.code", event.getExitCode()));
      }

      deleteCredentials(defaultCredentialsPath);
    }
  }
}
