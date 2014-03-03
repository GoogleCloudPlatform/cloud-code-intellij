/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.run;

import icons.GoogleCloudToolsIcons;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** Run Configuration Type for AppEngine Gradle modules */
public class AppEngineRunConfigurationType implements ConfigurationType {

  public static final String ID = "AppEngineRunConfiguration";

  private final AppEngineConfigurationFactory myFactory;

  public AppEngineRunConfigurationType() {
    myFactory = new AppEngineConfigurationFactory(this);
  }

  @Override
  public String getDisplayName() {
    return AppEngineRunConfiguration.NAME;
  }

  @Override
  public String getConfigurationTypeDescription() {
    return "Run App Engine module on the local development server";
  }

  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.AppEngine;
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myFactory};
  }

  public ConfigurationFactory getFactory() {
    return myFactory;
  }

  public static AppEngineRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(AppEngineRunConfigurationType.class);
  }

  private static class AppEngineConfigurationFactory extends ConfigurationFactory {

    public AppEngineConfigurationFactory(ConfigurationType type) {
      super(type);
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
      return new AppEngineRunConfiguration(AppEngineRunConfiguration.NAME, project, this);
    }

  }
}
