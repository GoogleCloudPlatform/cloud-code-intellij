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

package com.google.cloud.tools.intellij.debugger;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * This class defines the runconfig type and factory for Cloud Debugger RunConfigurations.
 */
public class CloudDebugConfigType implements ConfigurationType {

  public static final String GCT_DEBUGGER_ENABLE = "enable.gct.debugger";
  public static final String GCT_DEBUGGER_USETOKEN = "enable.gct.debugger.token";

  private final ConfigurationFactory factory;

  public static CloudDebugConfigType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(CloudDebugConfigType.class);
  }

  public CloudDebugConfigType() {
    factory = new MyConfigurationFactory(this);
  }

  public static boolean isFeatureEnabled() {
    return Boolean.getBoolean(GCT_DEBUGGER_ENABLE);
  }

  public static boolean useWaitToken() {
    return !Boolean.getBoolean(GCT_DEBUGGER_USETOKEN);
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{factory};
  }

  @Override
  public String getConfigurationTypeDescription() {
    return GctBundle.getString("clouddebug.text");
  }

  @Override
  public String getDisplayName() {
    return GctBundle.getString("clouddebug.text");
  }

  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.STACKDRIVER_DEBUGGER;
  }

  @NotNull
  @Override
  public String getId() {
    return "GCPDebug";
  }

  private static class MyConfigurationFactory extends ConfigurationFactory {

    MyConfigurationFactory(@NotNull CloudDebugConfigType type) {
      super(type);
    }

    @Override
    public void configureBeforeRunTaskDefaults(Key<? extends BeforeRunTask> providerId,
        BeforeRunTask task) {
      task.setEnabled(false);
    }

    @Override
    public RunConfiguration createTemplateConfiguration(final Project project) {
      return new CloudDebugRunConfiguration(project, this);
    }
  }
}
