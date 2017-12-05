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

package com.google.cloud.tools.intellij.debugger;

import com.google.cloud.tools.intellij.debugger.ui.CloudDebugRunConfigurationPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the UI to change Run Configuration settings and applies those to our {@link
 * CloudDebugRunConfiguration}.
 */
public class CloudDebugSettingsEditor extends SettingsEditor<CloudDebugRunConfiguration> {

  private final CloudDebugRunConfigurationPanel settingsPanel;

  public CloudDebugSettingsEditor() {
    settingsPanel = new CloudDebugRunConfigurationPanel();
  }

  @Override
  protected void applyEditorTo(CloudDebugRunConfiguration runConfiguration)
      throws ConfigurationException {
    runConfiguration.setCloudProjectName(settingsPanel.getProjectName());
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return settingsPanel.getMainPanel();
  }

  @Override
  protected void resetEditorFrom(CloudDebugRunConfiguration runConfiguration) {
    settingsPanel.setProjectName(runConfiguration.getCloudProjectName());
  }
}
