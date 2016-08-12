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

package com.google.cloud.tools.intellij.appengine.run;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * Created by joaomartins on 7/21/16.
 */
public class CloudSdkConfigurationType extends ConfigurationTypeBase {
  private static String displayName = "Google Cloud SDK Devappserver";
  private static String description = "Google Cloud SDK Dev App Server";
  private static Icon icon;
  private static String id = "CloudSdkDevAppServer";

  protected CloudSdkConfigurationType() {
    super(id, displayName, description, icon);
    addFactory(new CloudSdkConfigurationTypeFactory());
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getConfigurationTypeDescription() {
    return description;
  }

  @Override
  public Icon getIcon() {
    return icon;
  }

  @NotNull
  @Override
  public String getId() {
    return id;
  }

  public class CloudSdkConfigurationTypeFactory extends ConfigurationFactoryEx {

    public CloudSdkConfigurationTypeFactory() {
      super(CloudSdkConfigurationType.this);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new CloudSdkRunConfiguration(project, this);
    }
  }
}
