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

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginIcons;
import com.google.cloud.tools.intellij.project.ProjectLoader.ProjectLoaderResultCallback;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.table.JBTable;
import git4idea.DialogManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal project and account selection dialog. Contains account drop-down with user list, table with
 * project list and simple filter. Called from {@link ProjectSelector}.
 */
public class ProjectSelectionDialog extends DialogWrapper {

  private JComboBox<CredentialedUser> accountComboBox;
  private JButton addAccountButton;
  private JTextField filterTextField;
  private JTable projectListTable;
  private JPanel centerPanel;

  private CloudProject cloudProject;
  private ProjectListTableModel projectListTableModel;

  private ProjectLoader projectLoader;

   ProjectSelectionDialog(Component parent) {
    super(parent, false);
    init();
  }

  /**
   * Creates and shows modal dialog to select project/account. Blocks EDT until choice is made.
   *
   * @param cloudProject Current project selection to populate dialog UI state.
   * @return New project selection or null if user cancels.
   */
  @Nullable
  CloudProject showDialog(@Nullable CloudProject cloudProject) {
    loadUsersAndProjects();
    setCloudProject(cloudProject);
    DialogManager.show(this);
    int result = getExitCode();
    return result == OK_EXIT_CODE ? getCloudProject() : null;
  }

  @Override
  protected void init() {
    super.init();
    setTitle(GctBundle.getString("project.selector.dialog.title"));

    projectLoader = new ProjectLoader();
  }

  // IntelliJ API - creates actions (buttons) for "left side" of the dialog bottom panel.
  @NotNull
  @Override
  protected Action[] createLeftSideActions() {
    return new Action[] {new RefreshAction()};
  }

  private void createUIComponents() {
    // prepare account combobox model and rendering.
    accountComboBox = new ComboBox<>();
    accountComboBox.setRenderer(new AccountComboBoxRenderer());
    accountComboBox.addActionListener((event) -> updateProjectList());

    // prepare table model and rendering.
    projectListTable = new JBTable();
    projectListTableModel = new ProjectListTableModel();
    projectListTable.setModel(projectListTableModel);
    projectListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    projectListTable.getSelectionModel().addListSelectionListener(
        e -> validateProjectSelection());

    // disabled unless project is selected in the list.
    getOKAction().setEnabled(false);
  }

  private void setCloudProject(CloudProject cloudProject) {
    this.cloudProject = cloudProject;
    updateProjectAccountInformation();
  }

  private CloudProject getCloudProject() {
    CredentialedUser user = accountComboBox.getItemAt(accountComboBox.getSelectedIndex());
    CloudProject project = new CloudProject(getSelectedProjectName(), user.getEmail());
    return project;
  }

  private void loadUsersAndProjects() {
    Collection<CredentialedUser> credentialedUsers = Services.getLoginService().getAllUsers().values();
    if (credentialedUsers.isEmpty()) {
      showSignInRequest();
    } else {
      for (CredentialedUser user : credentialedUsers) {
        accountComboBox.addItem(user);
      }
      accountComboBox.setSelectedItem(Services.getLoginService().getActiveUser());
      updateProjectList();
    }
  }

  private void updateProjectList() {
    CredentialedUser user = (CredentialedUser) accountComboBox.getSelectedItem();
    if (user != null) {
      projectLoader.loadUserProjectsInBackground(
          user,
          new ProjectLoaderResultCallback() {
            @Override
            public void projectListReady(List<Project> result) {
              SwingUtilities.invokeLater(
                      () -> {
                        System.out.println(
                            "Received project list for "
                                + user.getEmail()
                                + ", size: "
                                + result.size());
                        projectListTableModel.setProjectList(result);
                      });
            }

            @Override
            public void onError(String errorDetails) {
              System.out.println("ERR!: " + errorDetails);
            }
          });
    }
  }

  private void updateProjectAccountInformation() {
    if (cloudProject != null) {
      Optional<CredentialedUser> loggedInUser =
          Services.getLoginService().getLoggedInUser(cloudProject.getGoogleUsername());
      if (loggedInUser.isPresent()) {
        accountComboBox.setSelectedItem(loggedInUser.get());
      } else {
        // specified user is not in logged in user list, clear the account selection.
        accountComboBox.setSelectedItem(null);
      }

      updateProjectList();
    }
  }

  private void showSignInRequest() {

  }

  private void validateProjectSelection() {
    if (projectListTable.getSelectedRow() >= 0) {
      setOKActionEnabled(true);
    } else {
      setOKActionEnabled(false);
    }
  }

  private String getSelectedProjectName() {
    return projectListTableModel.getValueAt(projectListTable.getSelectedRow(), ProjectListTableModel.PROJECT_NAME_COLUMN).toString();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return centerPanel;
  }

  private static final class RefreshAction extends AbstractAction {
    private RefreshAction() {
      putValue(Action.SMALL_ICON, GoogleCloudToolsIcons.REFRESH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {}
  }

  private static final class ProjectListTableModel extends AbstractTableModel {
    private static final int PROJECT_NAME_COLUMN = 0;
    private static final int PROJECT_ID_COLUMN = 1;

    private List<Project> projectList = new ArrayList<>();

    @Override
    public int getRowCount() {
      return projectList.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int row, int column) {
      switch (column) {
        case PROJECT_NAME_COLUMN:
          return projectList.get(row).getName();
        case PROJECT_ID_COLUMN:
          return projectList.get(row).getProjectId();
      }

      return "";
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case PROJECT_NAME_COLUMN:
          return GctBundle.getString("project.selector.project.list.project.name.column");
        case PROJECT_ID_COLUMN:
          return GctBundle.getString("project.selector.project.list.project.id.column");
      }

      return "";
    }

    private void setProjectList(
        List<Project> updatedList) {
      projectList.clear();
      projectList.addAll(updatedList);
      fireTableDataChanged();
    }
  }

  private static final class AccountComboBoxRenderer
      extends ListCellRendererWrapper<CredentialedUser> {

    @Override
    public void customize(
        JList list, CredentialedUser user, int index, boolean selected, boolean hasFocus) {
      if (user != null) {
        setText(user.getName() + " (" + user.getEmail() + ")");
        setIcon(
            GoogleLoginIcons.getScaledUserIcon(ProjectSelector.ACCOUNT_ICON_SIZE, user));
      }
    }
  }
}
