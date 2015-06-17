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
package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.elysium.ProjectSelector;
import com.intellij.ui.components.JBCheckBox;

import javax.swing.*;

/**
 * This is the config UI for cloud debug run configs.
 */
public class CloudDebugRunConfigurationPanel {
  private ProjectSelector myElysiumProjectId;
  private JPanel myPanel;
  private JBCheckBox myShowNotifications;

  public CloudDebugRunConfigurationPanel() {
  }

  public JPanel getMainPanel() {
    return myPanel;
  }

  public String getProjectName() {
    return myElysiumProjectId.getText();
  }

  public void setProjectName(String name) {
    myElysiumProjectId.setText(name);
  }

  public boolean getShowNotifications() {
    return myShowNotifications.isSelected();
  }

  public void setShowNotifications(boolean shouldShowNotifications) {
    myShowNotifications.setSelected(shouldShowNotifications);
  }
}
