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

package com.google.cloud.tools.intellij.appengine.server.run;

import com.google.cloud.tools.intellij.appengine.server.instance.AppEngineServerModel;
import com.google.cloud.tools.intellij.appengine.server.integration.AppEngineServerIntegration;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.javaee.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.run.configuration.J2EEConfigurationFactory;
import com.intellij.javaee.run.configuration.J2EEConfigurationType;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * @author nik
 */
public class AppEngineServerConfigurationType extends J2EEConfigurationType {

  public static AppEngineServerConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(AppEngineServerConfigurationType.class);
  }

  @SuppressWarnings("checkstyle:abbreviationAsWordInName")
  protected RunConfiguration createJ2EEConfigurationTemplate(ConfigurationFactory factory,
      Project project, boolean isLocal) {
    final AppEngineServerModel serverModel = new AppEngineServerModel();
    return J2EEConfigurationFactory.getInstance()
        .createJ2EERunConfiguration(factory, project, serverModel,
            getIntegration(), isLocal, new CloudSdkStartupPolicy());
  }

  public String getDisplayName() {
    return GctBundle.getString("appengine.run.server.name");
  }

  public String getConfigurationTypeDescription() {
    return getDisplayName() + " run configuration";
  }

  @Nullable
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{super.getConfigurationFactories()[0]};
  }

  public AppServerIntegration getIntegration() {
    return AppEngineServerIntegration.getInstance();
  }

  @NotNull
  public String getId() {
    return "GoogleAppEngineDevServer";
  }
}
