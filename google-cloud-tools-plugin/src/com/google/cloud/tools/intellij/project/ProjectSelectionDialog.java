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
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import git4idea.DialogManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
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
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jdesktop.swingx.sort.RowFilters.GeneralFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Modal project and account selection dialog. Contains account drop-down with user list, table with
 * project list and simple filter. {@link ProjectSelector} calls {@link #showDialog(CloudProject)}.
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
    projectListTable.getSelectionModel().addListSelectionListener(e -> validateProjectSelection());
    FilteredTextTableCellRenderer filterRenderer = new FilteredTextTableCellRenderer();
    projectListTable.setDefaultRenderer(Object.class, filterRenderer);

    // filter rows based on text field content.
    filterTextField = new JBTextField();
    TableRowSorter<TableModel> sorter = new TableRowSorter<>(projectListTableModel);
    projectListTable.setRowSorter(sorter);

    GeneralFilter filter =
        new GeneralFilter() {
          @Override
          protected boolean include(Entry value, int index) {
            return value
                .getStringValue(index)
                .toLowerCase()
                .contains(filterTextField.getText().toLowerCase());
          }
        };
    sorter.setRowFilter(filter);
    // on filter types, update row filter and renderer.
    filterTextField
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent e) {
                sorter.allRowsChanged();
                filterRenderer.setFilterText(filterTextField.getText());
              }
            });

    // disabled unless project is selected in the list.
    getOKAction().setEnabled(false);
  }

  private void setCloudProject(CloudProject cloudProject) {
    this.cloudProject = cloudProject;
    updateProjectAccountInformation();
  }

  private CloudProject getCloudProject() {
    CredentialedUser user = accountComboBox.getItemAt(accountComboBox.getSelectedIndex());
    return new CloudProject(getSelectedProjectName(), user.getEmail());
  }

  private void loadUsersAndProjects() {
    Collection<CredentialedUser> credentialedUsers =
        Services.getLoginService().getAllUsers().values();
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
      projectListTableModel.setProjectList(Collections.emptyList());
      ((JBTable) projectListTable).setPaintBusy(true);
      projectLoader.loadUserProjectsInBackground(
          user,
          new ProjectLoaderResultCallback() {
            @Override
            public void projectListReady(List<Project> result) {
              SwingUtilities.invokeLater(
                  () -> {
                    projectListTableModel.setProjectList(result);
                    ((JBTable) projectListTable).setPaintBusy(false);
                    showProjectInList(cloudProject.getProjectName());
                  });
            }

            @Override
            public void onError(String errorDetails) {
              ((JBTable) projectListTable).setPaintBusy(false);
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
      // no need to update project list - account combo box will generate event to reload.
    }
  }

  private void showSignInRequest() {}

  private void validateProjectSelection() {
    if (projectListTable.getSelectedRow() >= 0) {
      setOKActionEnabled(true);
    } else {
      setOKActionEnabled(false);
    }
  }

  // finds if project list contains the project with given name, selects and scrolls to it.
  private void showProjectInList(String projectName) {
    for (int i = 0; i < projectListTableModel.getRowCount(); i++) {
      String projectNameAtRow = projectListTableModel.getProjectNameAtRow(i);
      if (projectNameAtRow.equals(projectName)) {
        projectListTable.getSelectionModel().setSelectionInterval(i, i);
        projectListTable.scrollRectToVisible(projectListTable.getCellRect(i, 0, true));
        break;
      }
    }
  }

  private String getSelectedProjectName() {
    // row number change based on filtering state.
    int actualSelectedRow =
        projectListTable.getRowSorter().convertRowIndexToModel(projectListTable.getSelectedRow());
    return projectListTableModel.getProjectNameAtRow(actualSelectedRow);
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return centerPanel;
  }

  // re-queries all signed in users and projects.
  private final class RefreshAction extends AbstractAction {
    private RefreshAction() {
      putValue(Action.SMALL_ICON, GoogleCloudToolsIcons.REFRESH);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      loadUsersAndProjects();
    }
  }

  private static final class AccountComboBoxRenderer
      extends ListCellRendererWrapper<CredentialedUser> {

    @Override
    public void customize(
        JList list, CredentialedUser user, int index, boolean selected, boolean hasFocus) {
      if (user != null) {
        setText(user.getName() + " (" + user.getEmail() + ")");
        setIcon(GoogleLoginIcons.getScaledUserIcon(ProjectSelector.ACCOUNT_ICON_SIZE, user));
      }
    }
  }
}
