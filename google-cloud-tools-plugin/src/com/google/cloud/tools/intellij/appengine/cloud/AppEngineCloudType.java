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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkInstallSupport;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
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
    return GctBundle.message("appengine.name");
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
        callback.errorOccurred(GctBundle.message("appengine.deployment.error.not.logged.in"));
        return;
      }

      CloudSdkService cloudSdkService = CloudSdkService.getInstance();

      // check the status of SDK after install.
      SdkStatus sdkStatus =
          CloudSdkInstallSupport.getInstance().waitUntilCloudSdkInstalled(null);
      System.out.println("SDK Status post-install check: " + cloudSdkService.getStatus());
      switch (sdkStatus) {
        case INSTALLING:
          callback.errorOccurred(
              "Google Cloud SDK with App Engine Java needs to be completely installed to perform this action.");
          return;
        case NOT_AVAILABLE:
          callback.errorOccurred(
              "Google Cloud SDK is not available. Please check Settings -> Google -> Cloud SDK.");
          return;
        case INVALID:
          callback.errorOccurred(
              "Google Cloud SDK is invalid. Please check Settings -> Google -> Cloud SDK.");
          return;
        case READY:
          // can continue to deployment.
          break;
      }

      // TODO(ivanporty) need to show this in a better place once, based on an installation status.
      /*if (!CloudSdkValidator.getInstance().isValidCloudSdk()) {
        callback.errorOccurred(GctBundle.message("appengine.deployment.error.invalid.cloudsdk"));
        Notification invalidSdkWarning =
            new Notification(
                new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
                GctBundle.message("settings.menu.item.cloud.sdk.text"),
                GctBundle.message("appengine.deployment.error.invalid.cloudsdk"),
                NotificationType.ERROR);
        Notifications.Bus.notify(invalidSdkWarning);
        // TODO Consider auto opening configuration panel
      }*/

      callback.connected(new AppEngineRuntimeInstance());
    }
  }
}
