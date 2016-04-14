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
package com.google.cloud.tools.intellij;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType.UserSpecifiedPathDeploymentSourceType;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineToolsMenuAction;
import com.google.cloud.tools.intellij.debugger.CloudDebugConfigType;

import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerConfigurationType;

import org.jetbrains.annotations.NotNull;

/**
 * Performs runtime initialization for the GCT plugin.
 */
public class CloudToolsPluginInitializationComponent implements ApplicationComponent {

  @Override
  public void disposeComponent() {
    // Do nothing.
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleCloudToolsCore.InitializationComponent";
  }

  @Override
  public void initComponent() {
    CloudToolsPluginInfoService pluginInfoService = ServiceManager
        .getService(CloudToolsPluginInfoService.class);
    CloudToolsPluginConfigurationService pluginConfigurationService = ServiceManager
        .getService(CloudToolsPluginConfigurationService.class);
    if (pluginInfoService.shouldEnable(GctFeature.DEBUGGER)) {
      pluginConfigurationService
          .registerExtension(
              ConfigurationType.CONFIGURATION_TYPE_EP, new CloudDebugConfigType());
    }
    if (pluginInfoService.shouldEnable(GctFeature.APPENGINE_FLEX)) {
      AppEngineCloudType appEngineCloudType = new AppEngineCloudType();
      pluginConfigurationService.registerExtension(ServerType.EP_NAME, appEngineCloudType);
      pluginConfigurationService.registerExtension(DeploymentSourceType.EP_NAME,
          new UserSpecifiedPathDeploymentSourceType());
      pluginConfigurationService.registerExtension(ConfigurationType.CONFIGURATION_TYPE_EP,
          new DeployToServerConfigurationType(appEngineCloudType));

      ActionManager actionManager = ActionManager.getInstance();
      AnAction toolsMenuAction = new AppEngineToolsMenuAction();
      actionManager.registerAction(AppEngineToolsMenuAction.ID , toolsMenuAction);
      DefaultActionGroup toolsMenu =
          (DefaultActionGroup) actionManager.getAction(AppEngineToolsMenuAction.GROUP_ID);
      toolsMenu.add(toolsMenuAction, Constraints.LAST);
    }
    if (pluginInfoService.shouldEnableErrorFeedbackReporting()) {
      pluginConfigurationService
          .enabledGoogleFeedbackErrorReporting(pluginInfoService.getPluginId());
    }
  }
}
