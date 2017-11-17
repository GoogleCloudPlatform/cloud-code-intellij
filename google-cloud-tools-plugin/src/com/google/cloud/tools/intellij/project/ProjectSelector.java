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

package com.google.cloud.tools.intellij.project;

import com.google.cloud.tools.intellij.login.IntellijGoogleLoginService;
import com.google.cloud.tools.intellij.login.ui.CredentialedUserScaledIcon;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * ProjectSelector allows the user to select a GCP project id. It calls into {@link
 * IntellijGoogleLoginService} to get the set of credentialed users and then into resource manager
 * to get the set of projects.
 */
public class ProjectSelector extends JPanel {
  static final int ACCOUNT_ICON_SIZE = 16;

  private final List<ProjectSelectionListener> projectSelectionListeners = new ArrayList<>();

  private JBLabel projectNameLabel;
  private JBLabel accountInfoLabel;

  private CloudProject cloudProject;
  private JBLabel projectAccountSeparatorLabel;

  public ProjectSelector() {
    createUIComponents();
  }

  /** @return project selection or null if no project is selected. */
  public CloudProject getSelectedProject() {
    return cloudProject;
  }

  /**
   * Updates component UI and state with the new project and user account information.
   *
   * @param cloudProject New project/account information, null to clear selected project.
   */
  public void setSelectedProject(CloudProject cloudProject) {
    this.cloudProject = cloudProject;
    updateProjectAndUserInformation(cloudProject);
  }

  /**
   * Adds a {@link ProjectSelectionListener} to this class's internal list of listeners. All
   * ProjectSelectionListeners are notified when the selection is changed to a valid project.
   *
   * @param projectSelectionListener the listener to add
   */
  public void addProjectSelectionListener(ProjectSelectionListener projectSelectionListener) {
    projectSelectionListeners.add(projectSelectionListener);
  }

  /**
   * Removes a {@link ProjectSelectionListener} from this class's internal list of listeners.
   *
   * @param projectSelectionListener the listener to remove
   */
  public void removeProjectSelectionListener(ProjectSelectionListener projectSelectionListener) {
    projectSelectionListeners.remove(projectSelectionListener);
  }

  private void createUIComponents() {
    // layout - in the center of panel, horizontal line of project/account labels in a panel
    // ends with a fixed size browse button.
    setLayout(new BorderLayout());

    JPanel staticInfoPanel = new JPanel();
    staticInfoPanel.setLayout(new BoxLayout(staticInfoPanel, BoxLayout.X_AXIS));
    staticInfoPanel.setBorder(
        BorderFactory.createCompoundBorder(
            UIManager.getBorder("TextField.border"), BorderFactory.createEmptyBorder(2, 2, 2, 2)));

    projectNameLabel = new JBLabel();
    staticInfoPanel.add(projectNameLabel);
    staticInfoPanel.add(Box.createHorizontalStrut(5));
    projectAccountSeparatorLabel = new JBLabel("/");
    projectAccountSeparatorLabel.setVisible(false /* only visible when project is selected. */);
    staticInfoPanel.add(projectAccountSeparatorLabel);
    staticInfoPanel.add(Box.createHorizontalStrut(5));

    accountInfoLabel = new JBLabel();
    accountInfoLabel.setIcon(GoogleCloudToolsIcons.CLOUD_BREAKPOINT);
    staticInfoPanel.add(accountInfoLabel);
    staticInfoPanel.add(Box.createHorizontalGlue());

    ComponentWithBrowseButton<JPanel> componentWithBrowseButton =
        new ComponentWithBrowseButton<>(
            staticInfoPanel, (actionEvent) -> handleOpenProjectSelectionDialog());

    add(componentWithBrowseButton);
  }

  private void handleOpenProjectSelectionDialog() {
    CloudProject newSelection = ProjectSelectionDialog.showDialog(this, cloudProject);

    // if null, it means no change or user cancelled selection dialog - no update required.
    if (newSelection != null) {
      setSelectedProject(newSelection);
      notifyProjectSelectionListeners();
    }
  }

  private void updateProjectAndUserInformation(CloudProject selection) {
    if (selection == null) {
      projectNameLabel.setText(GctBundle.getString("project.selector.no.selected.project"));
      accountInfoLabel.setText("");
      accountInfoLabel.setIcon(null);
      projectAccountSeparatorLabel.setVisible(false);

    } else {
      projectNameLabel.setText(selection.getProject().getName());
      projectAccountSeparatorLabel.setVisible(true);
      accountInfoLabel.setText(
          selection.getUser().getName() + " (" + selection.getUser().getEmail() + ")");

      accountInfoLabel.setIcon(
          CredentialedUserScaledIcon.getScaledUserIcon(ACCOUNT_ICON_SIZE, selection.getUser()));
    }
  }

  private void notifyProjectSelectionListeners() {
    projectSelectionListeners.forEach(listener -> listener.projectSelected(cloudProject));
  }
}
