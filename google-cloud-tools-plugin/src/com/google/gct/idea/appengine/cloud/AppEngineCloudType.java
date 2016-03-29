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

import com.google.gct.idea.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.gct.idea.appengine.util.CloudSdkUtil;
import com.google.gct.idea.ui.GoogleCloudToolsIcons;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.Services;

import com.intellij.icons.AllIcons.FileTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
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
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;
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
 * This class hooks into IntelliJ's <a href="https://www.jetbrains.com/idea/help/clouds.html>Cloud</a>
 * configurations for infrastructure based deployment flows.
 */
public class AppEngineCloudType extends ServerType<AppEngineServerConfiguration> {

  public AppEngineCloudType() {
    super("gcp-app-engine"); // "google-app-engine" is used by the native IJ app engine support.
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return GctBundle.message("appengine.flex.name");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @NotNull
  @Override
  public AppEngineServerConfiguration createDefaultConfiguration() {
    return new AppEngineServerConfiguration();
  }

  @NotNull
  @Override
  public RemoteServerConfigurable createServerConfigurable(
      @NotNull AppEngineServerConfiguration configuration) {
    return new AppEngineCloudConfigurable(configuration, null);
  }

  @NotNull
  @Override
  public DeploymentConfigurator<?, AppEngineServerConfiguration> createDeploymentConfigurator(
      Project project) {
    return new AppEngineDeploymentConfigurator(project);
  }

  @NotNull
  @Override
  public ServerConnector<?> createConnector(@NotNull AppEngineServerConfiguration configuration,
      @NotNull ServerTaskExecutor asyncTasksExecutor) {
    return new AppEngineServerConnector(configuration);
  }

  protected static class AppEngineDeploymentConfigurator extends
      DeploymentConfigurator<AppEngineDeploymentConfiguration, AppEngineServerConfiguration> {

    private final Project project;

    public AppEngineDeploymentConfigurator(Project project) {
      this.project = project;
    }

    @NotNull
    @Override
    public List<DeploymentSource> getAvailableDeploymentSources() {
      List<DeploymentSource> deploymentSources = new ArrayList<DeploymentSource>();

      ModulePointer modulePointer =
          ModulePointerManager.getInstance(project).create("userSpecifiedSource");
      deploymentSources.add(new UserSpecifiedPathDeploymentSource(modulePointer));

      deploymentSources.addAll(JavaDeploymentSourceUtil
          .getInstance().createArtifactDeploymentSources(project, getJarsAndWars()));

      return deploymentSources;
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
    public AppEngineDeploymentConfiguration createDefaultConfiguration(
        @NotNull DeploymentSource source) {
      return new AppEngineDeploymentConfiguration();
    }

    @Nullable
    @Override
    public SettingsEditor<AppEngineDeploymentConfiguration> createEditor(
        @NotNull DeploymentSource source,
        @NotNull RemoteServer<AppEngineServerConfiguration> server) {
      return new AppEngineDeploymentRunConfigurationEditor(project, source,
          server.getConfiguration(),
          new CloudSdkAppEngineHelper(
              new File(CloudSdkUtil.toExecutablePath(server.getConfiguration().getCloudSdkHomePath())),
              server.getConfiguration().getCloudProjectName(),
              server.getConfiguration().getGoogleUserName()));
    }

    /**
     * A deployment source used as a placeholder to allow user selection of a jar or war file from
     * the filesystem.
     */
    protected static class UserSpecifiedPathDeploymentSource extends ModuleDeploymentSourceImpl {
      private String name =
          GctBundle.message("appengine.flex.user.specified.deploymentsource.name");

      public UserSpecifiedPathDeploymentSource(@NotNull ModulePointer pointer) {
        super(pointer);
      }

      @Override
      public boolean isValid() {
        return false;
      }

      @Nullable
      @Override
      public Icon getIcon() {
        return FileTypes.Any_type;
      }

      @NotNull
      @Override
      public String getPresentableName() {
        return name;
      }

      public void setName(String name) {
        this.name = name;
      }
    }
  }

  private static class AppEngineServerConnector extends
      ServerConnector<AppEngineDeploymentConfiguration> {

    private AppEngineServerConfiguration configuration;

    public AppEngineServerConnector(
        AppEngineServerConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public void connect(@NotNull ConnectionCallback<AppEngineDeploymentConfiguration> callback) {
      if (CloudSdkUtil.isCloudSdkExecutable(CloudSdkUtil.toExecutablePath(configuration.getCloudSdkHomePath()))) {
        callback.connected(new AppEngineRuntimeInstance(configuration));
      } else {
        callback.errorOccurred(GctBundle.message("appengine.deployment.error.invalid.cloudsdk"));
        // TODO Consider auto opening configuration panel
      }
    }
  }

  private static class AppEngineRuntimeInstance extends
      ServerRuntimeInstance<AppEngineDeploymentConfiguration> {

    private AppEngineServerConfiguration configuration;

    public AppEngineRuntimeInstance(
        AppEngineServerConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public void deploy(@NotNull final DeploymentTask<AppEngineDeploymentConfiguration> task,
        @NotNull final DeploymentLogManager logManager,
        @NotNull final DeploymentOperationCallback callback) {
      FileDocumentManager.getInstance().saveAllDocuments();
      if (!Services.getLoginService().isLoggedIn()) {
        callback.errorOccurred(GctBundle.message("appengine.deployment.error.not.logged.in"));
        return;
      }
      AppEngineHelper appEngineHelper = new CloudSdkAppEngineHelper(
          getFileFromFilePath(CloudSdkUtil.toExecutablePath(configuration.getCloudSdkHomePath())),
          configuration.getCloudProjectName(),
          configuration.getGoogleUserName());

      final Runnable doDeployment;
      AppEngineDeploymentConfiguration deploymentConfig = task.getConfiguration();
      File deploymentSource = deploymentConfig.isUserSpecifiedArtifact() ?
          new File(deploymentConfig.getUserSpecifiedArtifactPath()) : task.getSource().getFile();

      if (deploymentConfig.getConfigType() == ConfigType.AUTO) {
        doDeployment = appEngineHelper.createAutoDeploymentOperation(
            logManager.getMainLoggingHandler(),
            deploymentSource,
            callback
        );
      } else {
        doDeployment = appEngineHelper.createCustomDeploymentOperation(
            logManager.getMainLoggingHandler(),
            deploymentSource,
            getFileFromFilePath(deploymentConfig.getAppYamlPath()),
            getFileFromFilePath(deploymentConfig.getDockerFilePath()),
            callback
        );
      }
      ProgressManager.getInstance()
          .run(new Task.Backgroundable(task.getProject(), GctBundle.message(
              "appengine.deployment.status.deploying"), true,
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
