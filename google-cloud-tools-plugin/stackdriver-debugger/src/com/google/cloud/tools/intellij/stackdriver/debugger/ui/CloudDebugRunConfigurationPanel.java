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

package com.google.cloud.tools.intellij.stackdriver.debugger.ui;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.stackdriver.debugger.StackdriverDebuggerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBCheckBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** This is the config UI for cloud debug run configs. */
public class CloudDebugRunConfigurationPanel {

  private JPanel panel;
  private HyperlinkLabel docsLink;
  private JLabel description;
  private ProjectSelector projectSelector;
  private JCheckBox hiddenValidationTrigger;

  private Project ideProject;

  public CloudDebugRunConfigurationPanel(Project ideProject) {
    this.ideProject = ideProject;

    docsLink.setHyperlinkText(
        StackdriverDebuggerBundle.message("clouddebug.runconfig.formoredetails"),
        StackdriverDebuggerBundle.message("clouddebug.runconfig.documentation.url.text"),
        ".");
    docsLink.setHyperlinkTarget(
        StackdriverDebuggerBundle.message("clouddebug.runconfig.documentation.url"));
    description.setText(StackdriverDebuggerBundle.message("clouddebug.runconfig.description"));
  }

  public JPanel getMainPanel() {
    return panel;
  }

  public CloudProject getSelectedCloudProject() {
    return projectSelector.getSelectedProject();
  }

  public void setSelectedCloudProject(CloudProject cloudProject) {
    projectSelector.setSelectedProject(cloudProject);
  }

  public void loadActiveCloudProject() {
    projectSelector.loadActiveCloudProject();
  }

  private void triggerValidation() {
    hiddenValidationTrigger.doClick();
  }

  private void createUIComponents() {
    hiddenValidationTrigger = new JBCheckBox();
    hiddenValidationTrigger.setVisible(false);

    projectSelector = new ProjectSelector(ideProject);
    projectSelector.addProjectSelectionListener(
        (selectedProject) -> {
          // settings editor does not see all the changes by default, use explicit notification.
          triggerValidation();
        });
  }
}
