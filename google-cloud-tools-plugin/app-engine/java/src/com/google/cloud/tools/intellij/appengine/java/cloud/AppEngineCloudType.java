/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.login.Services;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

/**
 * This class hooks into IntelliJ's <a
 * href="https://www.jetbrains.com/idea/help/clouds.html>Cloud</a> configurations for infrastructure
 * based deployment flows.
 */
public class AppEngineCloudType extends ServerType<AppEngineServerConfiguration> {

  /** Initialize the App Engine Cloud Type and handle cleanup. */
  public AppEngineCloudType() {
    super("gcp-app-engine"); // "google-app-engine" is used by the native IJ app engine support.
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return AppEngineMessageBundle.message("appengine.name");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
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
    return new AppEngineCloudConfigurable();
  }

  @NotNull
  @Override
  public DeploymentConfigurator<?, AppEngineServerConfiguration> createDeploymentConfigurator(
      Project project) {
    return new AppEngineDeploymentConfigurator(project);
  }

  @NotNull
  @Override
  public ServerConnector<?> createConnector(
      @NotNull AppEngineServerConfiguration configuration,
      @NotNull ServerTaskExecutor asyncTasksExecutor) {
    return new AppEngineServerConnector();
  }

  public static class AppEngineServerConnector
      extends ServerConnector<AppEngineDeploymentConfiguration> {

    @Override
    public void connect(@NotNull ConnectionCallback<AppEngineDeploymentConfiguration> callback) {
      Services.getLoginService().logInIfNot();

      if (!Services.getLoginService().isLoggedIn()) {
        callback.errorOccurred(
            AppEngineMessageBundle.message("appengine.deployment.error.not.logged.in"));
        return;
      }

      callback.connected(new AppEngineRuntimeInstance());
    }
  }
}
