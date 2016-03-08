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

import com.google.gct.idea.appengine.cloud.ManagedVmDeploymentConfiguration.ConfigType;
import com.google.gct.idea.appengine.util.CloudSdkUtil;
import com.google.gct.idea.ui.GoogleCloudToolsIcons;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.JavaDeploymentSourceUtil;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;


/**
 * This class hooks into IntelliJ's
 * <a href="https://www.jetbrains.com/idea/help/clouds.html>Cloud</a> configurations for
 * infrastructure based deployment flows.
 */
public class ManagedVmCloudType extends ServerType<ManagedVmServerConfiguration> {

  public ManagedVmCloudType() {
    super("app-engine-managed-vm");
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return GctBundle.message("appengine.managedvm.name");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @NotNull
  @Override
  public ManagedVmServerConfiguration createDefaultConfiguration() {
    return new ManagedVmServerConfiguration();
  }

  @NotNull
  @Override
  public RemoteServerConfigurable createServerConfigurable(
      @NotNull ManagedVmServerConfiguration configuration) {
    return new ManagedVmCloudConfigurable(configuration, null);
  }

  @NotNull
  @Override
  public DeploymentConfigurator<?, ManagedVmServerConfiguration> createDeploymentConfigurator(
      Project project) {
    return new ManagedVmDeploymentConfigurator(project);
  }

  @NotNull
  @Override
  public ServerConnector<?> createConnector(@NotNull ManagedVmServerConfiguration configuration,
      @NotNull ServerTaskExecutor asyncTasksExecutor) {
    return new ManagedVmServerConnector(configuration);
  }

  private static class ManagedVmDeploymentConfigurator extends
      DeploymentConfigurator<ManagedVmDeploymentConfiguration, ManagedVmServerConfiguration> {

    private final Project project;

    public ManagedVmDeploymentConfigurator(Project project) {
      this.project = project;
    }

    @NotNull
    @Override
    public List<DeploymentSource> getAvailableDeploymentSources() {
      return JavaDeploymentSourceUtil
          .getInstance().createArtifactDeploymentSources(project, getJarsAndWars());
    }

    private List<Artifact> getJarsAndWars() {
      List<Artifact> jarsAndWars = new ArrayList<Artifact>();
      for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
        if (artifact.getArtifactType().getId().equalsIgnoreCase("jar")
            || artifact.getArtifactType().getId().equalsIgnoreCase("war")) {
          jarsAndWars.add(artifact);
        }
      }

      Collections.sort(jarsAndWars, ArtifactManager.ARTIFACT_COMPARATOR);
      return jarsAndWars;
    }

    @NotNull
    @Override
    public ManagedVmDeploymentConfiguration createDefaultConfiguration(
        @NotNull DeploymentSource source) {
      return new ManagedVmDeploymentConfiguration();
    }

    @Nullable
    @Override
    public SettingsEditor<ManagedVmDeploymentConfiguration> createEditor(
        @NotNull DeploymentSource source,
        @NotNull RemoteServer<ManagedVmServerConfiguration> server) {
      return new ManagedVmDeploymentRunConfigurationEditor(project, source,
          new CloudSdkAppEngineHelper(new File(server.getConfiguration().getCloudSdkExecutablePath()),
              server.getConfiguration().getCloudProjectName()));
    }
  }

  private static class ManagedVmServerConnector extends
      ServerConnector<ManagedVmDeploymentConfiguration> {

    private ManagedVmServerConfiguration configuration;

    public ManagedVmServerConnector(
        ManagedVmServerConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public void connect(@NotNull ConnectionCallback<ManagedVmDeploymentConfiguration> callback) {
      if (CloudSdkUtil.isCloudSdkExecutable(configuration.getCloudSdkExecutablePath())) {
        callback.connected(new ManagedVmRuntimeInstance(configuration));
      }
      else {
        callback.errorOccurred("Invalid Cloud SDK directory path configured.");
        // TODO Consider auto opening configuration panel
      }
    }
  }

  private static class ManagedVmRuntimeInstance extends
      ServerRuntimeInstance<ManagedVmDeploymentConfiguration> {

    private ManagedVmServerConfiguration configuration;

    public ManagedVmRuntimeInstance(
        ManagedVmServerConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public void deploy(@NotNull final DeploymentTask<ManagedVmDeploymentConfiguration> task,
        @NotNull final DeploymentLogManager logManager,
        @NotNull final DeploymentOperationCallback callback) {
      FileDocumentManager.getInstance().saveAllDocuments();

      AppEngineHelper appEngineHelper = new CloudSdkAppEngineHelper(
          getFileFromFilePath(configuration.getCloudSdkExecutablePath()),
          configuration.getCloudProjectName()
      );

      final Runnable doDeployment;
      ManagedVmDeploymentConfiguration deploymentConfig = task.getConfiguration();
      if (deploymentConfig.getConfigType() == ConfigType.AUTO) {
        doDeployment = appEngineHelper.createAutoDeploymentOperation(
            logManager.getMainLoggingHandler(),
            task.getSource().getFile(),
            callback
        );
      } else {
        doDeployment = appEngineHelper.createCustomDeploymentOperation(
            logManager.getMainLoggingHandler(),
            task.getSource().getFile(),
            getFileFromFilePath(deploymentConfig.getAppYamlPath()),
            getFileFromFilePath(deploymentConfig.getDockerFilePath()),
            callback
        );
      }
      ProgressManager.getInstance()
          .run(new Task.Backgroundable(task.getProject(), "Deploying to MVM", true,
              null) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              ApplicationManager.getApplication().invokeLater(doDeployment);
            }
          });
    }

    @Override
    public void computeDeployments(@NotNull ComputeDeploymentsCallback callback) {
    }

    @Override
    public void disconnect() {
    }

    @NotNull
    private File getFileFromFilePath(String filePath) {
      File file;
      file = new File(filePath);
      if (!file.exists()) {
        throw new RuntimeException(filePath + " does not exist");
      }
      return file;
    }
  }
}
