/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import javax.swing.Icon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/** An App Engine standard Community Edition {@link ConfigurationType} implementation. */
public class AppEngineLocalServerCommunityConfigurationType implements ConfigurationType {

  private static final String ID = "gcp-app-engine-local-run-ce";
  private final AppEngineConfigurationFactory factory;

  public AppEngineLocalServerCommunityConfigurationType() {
    factory = new AppEngineConfigurationFactory(this);
  }

  @Nls
  @Override
  public String getDisplayName() {
    return AppEngineMessageBundle.getString("appengine.run.server.name");
  }

  @Nls
  @Override
  public String getConfigurationTypeDescription() {
    return getDisplayName() + " run configuration";
  }

  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[] {factory};
  }

  private static class AppEngineConfigurationFactory extends ConfigurationFactory {

    AppEngineConfigurationFactory(@NotNull ConfigurationType type) {
      super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new AppEngineCommunityRunConfiguration(
          AppEngineMessageBundle.getString("appengine.run.server.name"), project, this);
    }
  }
}
