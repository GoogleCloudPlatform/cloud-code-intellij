/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.gct.idea.debugger.ui.CloudDebugRunConfigurationPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The CloudDebugSettingsEditor creates the UI to change Run Configuration settings and applies those to our {@link
 * com.google.gct.idea.debugger.CloudDebugRunConfiguration}
 */
public class CloudDebugSettingsEditor extends SettingsEditor<CloudDebugRunConfiguration> {
  private final CloudDebugRunConfigurationPanel mySettingPanel;

  public CloudDebugSettingsEditor() {
    mySettingPanel = new CloudDebugRunConfigurationPanel();
  }

  @Override
  protected void applyEditorTo(CloudDebugRunConfiguration s) throws ConfigurationException {
    s.setCloudProjectName(mySettingPanel.getProjectName());
    s.setShowNotifications(mySettingPanel.getShowNotifications());

  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return mySettingPanel.getMainPanel();
  }

  @Override
  protected void resetEditorFrom(CloudDebugRunConfiguration s) {
    mySettingPanel.setProjectName(s.getCloudProjectName());
    mySettingPanel.setShowNotifications(s.isShowNotifications());
  }
}
