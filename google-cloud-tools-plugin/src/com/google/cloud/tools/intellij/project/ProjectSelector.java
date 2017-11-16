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
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * ProjectSelector allows the user to select a GCP project id. It calls into {@link
 * IntellijGoogleLoginService} to get the set of credentialed users and then into resource manager
 * to get the set of projects.
 */
public class ProjectSelector extends JPanel {
  private final List<ProjectSelectionListener> projectSelectionListeners = new ArrayList<>();

  private JBLabel projectNameLabel;
  private JBLabel accoountInfoLabel;

  public ProjectSelector() {
    createUIComponents();
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
    setLayout(new BorderLayout());

    JPanel staticInfoPanel = new JPanel();
    staticInfoPanel.setLayout(new BoxLayout(staticInfoPanel, BoxLayout.X_AXIS));
    staticInfoPanel.setBorder(UIManager.getBorder("TextField.border"));

    projectNameLabel = new JBLabel("test-project");
    staticInfoPanel.add(projectNameLabel);
    staticInfoPanel.add(Box.createHorizontalStrut(5));
    staticInfoPanel.add(new JBLabel("/"));
    staticInfoPanel.add(Box.createHorizontalStrut(5));

    accoountInfoLabel = new JBLabel("John Doe (jd@google.com)");
    accoountInfoLabel.setIcon(GoogleCloudToolsIcons.CLOUD_BREAKPOINT);
    staticInfoPanel.add(accoountInfoLabel);
    staticInfoPanel.add(Box.createHorizontalGlue());

    ComponentWithBrowseButton<JPanel> componentWithBrowseButton =
        new ComponentWithBrowseButton<>(
            staticInfoPanel, (actionEvent) -> handleOpenProjectSelectionDialog());

    add(componentWithBrowseButton);
  }

  private void handleOpenProjectSelectionDialog() {
    int result = ProjectSelectionDialog.showDialog(this);
    if (result == DialogWrapper.OK_EXIT_CODE) {
      System.out.println("OK!");
    }
  }

  private void updateProjectAndUserInformation(ProjectSelection selection) {
    projectNameLabel.setText(selection.getProject().getName());
    accoountInfoLabel.setText(selection.getUser().getName());
    accoountInfoLabel.setIcon(new ImageIcon(selection.getUser().getPicture()));
  }
}
