/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.startup;

import com.google.cloud.tools.intellij.CloudToolsPluginConfigurationService;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
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
    CloudToolsPluginConfigurationService pluginConfigurationService = ServiceManager
        .getService(CloudToolsPluginConfigurationService.class);
    CloudToolsPluginInfoService pluginInfoService = ServiceManager
        .getService(CloudToolsPluginInfoService.class);

    if (pluginInfoService.shouldEnableErrorFeedbackReporting()) {
      initErrorReporting(pluginConfigurationService, pluginInfoService);
    }

    new ConflictingAppEnginePluginCheck().notifyIfConflicting();
  }

  private void initErrorReporting(CloudToolsPluginConfigurationService pluginConfigurationService,
      CloudToolsPluginInfoService pluginInfoService) {
    pluginConfigurationService
        .enabledGoogleFeedbackErrorReporting(pluginInfoService.getPluginId());
  }
}
