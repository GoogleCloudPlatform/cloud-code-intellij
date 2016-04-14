/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.login.Services;

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
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.JavaDeploymentSourceUtil;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;
import com.intellij.remoteServer.runtime.ServerConnection;
import com.intellij.remoteServer.runtime.ServerConnectionManager;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;


/**
 * This class hooks into IntelliJ's <a href="https://www.jetbrains.com/idea/help/clouds.html>Cloud</a>
 * configurations for infrastructure based deployment flows.
 */
public class AppEngineCloudType extends ServerType<AppEngineServerConfiguration> {

  public AppEngineCloudType() {
    super("gcp-app-engine"); // "google-app-engine" is used by the native IJ app engine support.

    // listen for project closing event and close all active server connections
    ProjectManager projectManager = ProjectManager.getInstance();
    if (projectManager != null) {
      projectManager.addProjectManagerListener(new ProjectManagerAdapter() {
        @Override
        public void projectClosing(Project project) {
          super.projectClosing(project);
          for (ServerConnection connection : ServerConnectionManager.getInstance()
              .getConnections()) {
            if (connection.getServer().getType() instanceof AppEngineCloudType) {
              connection.disconnect();
            }
          }
        }
      });
    }

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
      private String userSpecifiedFilePath;

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

      @Nullable
      @Override
      public File getFile() {
        return userSpecifiedFilePath != null ? new File(userSpecifiedFilePath) : null;
      }

      public void setFilePath(@NotNull String userSpecifiedFilePath) {
        this.userSpecifiedFilePath = userSpecifiedFilePath;
      }

      @NotNull
      @Override
      public DeploymentSourceType<?> getType() {
        return DeploymentSourceType.EP_NAME.findExtension(
            UserSpecifiedPathDeploymentSourceType.class);
      }
    }
  }

  public static class UserSpecifiedPathDeploymentSourceType extends
      DeploymentSourceType<ModuleDeploymentSource> {
    private static final String NAME_ATTRIBUTE = "name";
    private static final String SOURCE_TYPE_ID = "filesystem-war-jar-module";

    public UserSpecifiedPathDeploymentSourceType() { super(SOURCE_TYPE_ID); }

    /**
     * Restore presentable name (e.g., to be "Filesystem JAR or WAR file - <file path>") of
     * UserSpecifiedPathDeploymentSource.
     */
    @NotNull
    @Override
    public ModuleDeploymentSource load(@NotNull Element tag, @NotNull Project project) {
      AppEngineDeploymentConfigurator.UserSpecifiedPathDeploymentSource userSpecifiedSource =
          new AppEngineDeploymentConfigurator.UserSpecifiedPathDeploymentSource(ModulePointerManager
              .getInstance(project).create(tag.getAttributeValue(NAME_ATTRIBUTE)));

      Element settings = tag.getChild(DeployToServerRunConfiguration.SETTINGS_ELEMENT);
      if (settings != null) {
        String filePath = settings.getAttributeValue(
            AppEngineDeploymentConfiguration.USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE);

        if (!StringUtil.isEmpty(filePath)) {
          userSpecifiedSource.setName(
              GctBundle.message(
                  "appengine.flex.user.specified.deploymentsource.name.with.filename",
                  new File(filePath).getName()));

          return userSpecifiedSource;
        }
      }

      userSpecifiedSource.setName(
          GctBundle.message("appengine.flex.user.specified.deploymentsource.name"));

      return userSpecifiedSource;
    }

    @Override
    public void save(@NotNull ModuleDeploymentSource source, @NotNull Element tag) {
      tag.setAttribute(NAME_ATTRIBUTE, source.getModulePointer().getModuleName());
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
      if (!Services.getLoginService().isLoggedIn()) {
        callback.errorOccurred(GctBundle.message("appengine.deployment.error.not.logged.in"));
      } else if (CloudSdkUtil.isCloudSdkExecutable(CloudSdkUtil.toExecutablePath(configuration.getCloudSdkHomePath()))) {
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
    private Set<AppEngineAction> createdDeployments;

    public AppEngineRuntimeInstance(
        AppEngineServerConfiguration configuration) {
      this.configuration = configuration;
      this.createdDeployments = new HashSet<>();
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
      String gcloudCommandPath = CloudSdkUtil.toExecutablePath(configuration.getCloudSdkHomePath());
      File gcloudCommand = getFileFromFilePath(gcloudCommandPath);
      AppEngineHelper appEngineHelper = new CloudSdkAppEngineHelper(
          gcloudCommand,
          configuration.getCloudProjectName(),
          configuration.getGoogleUserName());

      final AppEngineDeployAction deployAction;
      AppEngineDeploymentConfiguration deploymentConfig = task.getConfiguration();
      File deploymentSource = deploymentConfig.isUserSpecifiedArtifact() ?
          new File(deploymentConfig.getUserSpecifiedArtifactPath()) : task.getSource().getFile();

      if (deploymentConfig.getConfigType() == ConfigType.AUTO) {
        deployAction = appEngineHelper.createAutoDeploymentAction(
            logManager.getMainLoggingHandler(),
            task.getProject(),
            deploymentSource,
            callback
        );
      } else {
        deployAction = appEngineHelper.createCustomDeploymentAction(
            logManager.getMainLoggingHandler(),
            task.getProject(),
            deploymentSource,
            getFileFromFilePath(deploymentConfig.getAppYamlPath()),
            getFileFromFilePath(deploymentConfig.getDockerFilePath()),
            deploymentConfig.getVersion(),
            callback
        );
      }

      // keep track of any active deployments
      createdDeployments.add(deployAction);

      ProgressManager.getInstance()
          .run(new Task.Backgroundable(task.getProject(), GctBundle.message(
              "appengine.deployment.status.deploying"), true,
              null) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
              ApplicationManager.getApplication().invokeLater(deployAction);
            }
          });
    }

    @Override
    public void computeDeployments(@NotNull ComputeDeploymentsCallback callback) {
    }

    @Override
    public synchronized void disconnect() {
      // kill any executing actions
      for (AppEngineAction action : createdDeployments) {
        action.cancel();
      }
      createdDeployments.clear();
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