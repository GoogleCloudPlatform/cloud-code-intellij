/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming GCP API management actions such as API enablement. Allows selection of a {@link
 * CloudProject CloudProject} on which to perform the actions.
 */
public class CloudApiManagementDialog extends DialogWrapper {

  private JPanel panel;
  private ProjectSelector projectSelector;

  /**
   * Initializes the dialog and enables the OK button if a {@link CloudProject CloudProject} is
   * selected and disables it otherwise.
   */
  CloudApiManagementDialog(@Nullable Project project) {
    super(project);
    init();
    setTitle(GctBundle.message("cloud.apis.management.dialog.title"));

    setOKActionEnabled(getCloudProject() != null);
    projectSelector.addProjectSelectionListener(cloudProject -> setOKActionEnabled(true));
  }

  @Nullable
  CloudProject getCloudProject() {
    return projectSelector.getSelectedProject();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  @VisibleForTesting
  public ProjectSelector getProjectSelector() {
    return projectSelector;
  }
}
