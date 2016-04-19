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
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.runtime.ServerConnection;
import com.intellij.remoteServer.runtime.ServerConnectionManager;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;

import org.jetbrains.annotations.NotNull;

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

  private static class AppEngineServerConnector extends
      ServerConnector<AppEngineDeploymentConfiguration> {

    private AppEngineServerConfiguration configuration;

    private AppEngineServerConnector(
        AppEngineServerConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public void connect(@NotNull ConnectionCallback<AppEngineDeploymentConfiguration> callback) {
      Services.getLoginService().logInIfNot();

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

}