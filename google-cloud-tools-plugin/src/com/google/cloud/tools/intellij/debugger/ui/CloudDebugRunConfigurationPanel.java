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

package com.google.cloud.tools.intellij.debugger.ui;

import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.ui.HyperlinkLabel;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This is the config UI for cloud debug run configs.
 */
public class CloudDebugRunConfigurationPanel {

  private ProjectSelector elysiumProjectId;
  private JPanel panel;
  private HyperlinkLabel docsLink;
  private JLabel description;

  public CloudDebugRunConfigurationPanel() {
    docsLink.setHyperlinkText(
        GctBundle.message("clouddebug.runconfig.formoredetails"),
        GctBundle.message("clouddebug.runconfig.documentation.url.text"),
        ".");
    docsLink.setHyperlinkTarget(GctBundle.message("clouddebug.runconfig.documentation.url"));
    description.setText(GctBundle.message("clouddebug.runconfig.description"));
  }

  public JPanel getMainPanel() {
    return panel;
  }

  public String getProjectName() {
    return elysiumProjectId.getText();
  }

  public void setProjectName(String name) {
    elysiumProjectId.setText(name);
  }
}
