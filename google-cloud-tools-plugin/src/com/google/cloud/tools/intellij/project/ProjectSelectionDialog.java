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
import com.google.cloud.tools.intellij.login.GoogleLoginListener;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginIcons;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import git4idea.DialogManager;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
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
public class ProjectSelectionDialog {

  private JComboBox<CredentialedUser> accountComboBox;
  private JButton addAccountButton;
  private JTextField filterTextField;
  private JTable projectListTable;
  private JPanel centerPanel;

  private ProjectSelectionDialogWrapper dialogWrapper;

  private JPanel centerPanelWrapper;
  private ProjectSelectorSignInPanel signInScreen = new ProjectSelectorSignInPanel();

  private RefreshAction refreshAction;

  private CloudProject cloudProject;
  private ProjectListTableModel projectListTableModel;

  private ProjectLoader projectLoader = new ProjectLoader();

  /**
   * Creates and shows modal dialog to select project/account. Blocks EDT until choice is made.
   *
   * @param cloudProject Current project selection to populate dialog UI state.
   * @return New project selection or null if user cancels.
   */
  @Nullable
  CloudProject showDialog(@Nullable CloudProject cloudProject) {
    // create dialog wrapper for the form once, create services and subscribe to login changes once.
    if (dialogWrapper == null) {
      dialogWrapper = new ProjectSelectionDialogWrapper();

      dialogWrapper.setTitle(GctBundle.getString("project.selector.dialog.title"));
      // disabled unless project is selected in the list.
      dialogWrapper.setOKActionEnabled(false);

      Stream.of(ProjectManager.getInstance().getOpenProjects())
          .forEach(
              project ->
                  project
                      .getMessageBus()
                      .connect(dialogWrapper.getDisposable() /* disconnect once dialog is gone. */)
                      .subscribe(
                          GoogleLoginListener.GOOGLE_LOGIN_LISTENER_TOPIC,
                          this::loadUsersAndProjects));
    }

    loadUsersAndProjects();
    setCloudProject(cloudProject);

    DialogManager.show(dialogWrapper);

    int result = dialogWrapper.getExitCode();
    return result == DialogWrapper.OK_EXIT_CODE ? getCloudProject() : null;
  }

  @VisibleForTesting
  void createUIComponents() {
    addAccountButton = new JButton();
    addAccountButton.addActionListener((event) -> Services.getLoginService().logIn());

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

    refreshAction = new RefreshAction();

    // prepare account combobox model and rendering.
    accountComboBox = new ComboBox<>();
    accountComboBox.setRenderer(new AccountComboBoxRenderer());
    accountComboBox.addActionListener((event) -> updateProjectList());

    // wrapper for center panel that holds either project selection or sign in screen.
    centerPanelWrapper = new JPanel(new BorderLayout());
  }

  @VisibleForTesting
  void setCloudProject(CloudProject cloudProject) {
    this.cloudProject = cloudProject;
    updateProjectAccountInformation();
  }

  private CloudProject getCloudProject() {
    CredentialedUser user = accountComboBox.getItemAt(accountComboBox.getSelectedIndex());
    return CloudProject.create(getSelectedProjectName(), user.getEmail());
  }

  @VisibleForTesting
  void loadUsersAndProjects() {
    Collection<CredentialedUser> credentialedUsers =
        Services.getLoginService().getAllUsers().values();
    if (credentialedUsers.isEmpty()) {
      showSignInRequest();
    } else {
      hideSignInRequest();
      accountComboBox.removeAllItems();
      for (CredentialedUser user : credentialedUsers) {
        accountComboBox.addItem(user);
      }
      accountComboBox.setSelectedItem(Services.getLoginService().getActiveUser());
      // no need to update project list - account combo box will generate an event.
    }
  }

  private void updateProjectList() {
    // clear list and reload for selected user, empty if user is not logged in/selection empty.
    projectListTableModel.setProjectList(Collections.emptyList());
    CredentialedUser user = (CredentialedUser) accountComboBox.getSelectedItem();
    if (user != null) {
      ((JBTable) projectListTable).setPaintBusy(true);
      ListenableFuture<List<Project>> futureResult =
          projectLoader.loadUserProjectsInBackground(user);
      addProjectListFutureCallback(
          futureResult,
          new FutureCallback<List<Project>>() {
            @Override
            public void onSuccess(List<Project> projects) {
              SwingUtilities.invokeLater(
                  () -> {
                    projectListTableModel.setProjectList(projects);
                    ((JBTable) projectListTable).setPaintBusy(false);
                    if (cloudProject != null) {
                      showProjectInList(cloudProject.projectName());
                    }
                  });
            }

            @Override
            public void onFailure(Throwable throwable) {
              ((JBTable) projectListTable).setPaintBusy(false);
              dialogWrapper.setErrorInfoAll(
                  Collections.singletonList(new ValidationInfo(throwable.getMessage())));
            }
          });
    }
  }

  private void updateProjectAccountInformation() {
    if (cloudProject != null && !Strings.isNullOrEmpty(cloudProject.googleUsername())) {
      Optional<CredentialedUser> loggedInUser =
          Services.getLoginService().getLoggedInUser(cloudProject.googleUsername());
      if (loggedInUser.isPresent()) {
        accountComboBox.setSelectedItem(loggedInUser.get());
      } else {
        // specified user is not in logged in user list, clear the account selection.
        accountComboBox.setSelectedItem(null);
      }
      // no need to update project list - account combo box will generate an event.
    }
  }

  private void showSignInRequest() {
    centerPanelWrapper.removeAll();
    centerPanelWrapper.add(signInScreen);
    getDialogButton(refreshAction).setVisible(false);
    dialogWrapper.validate();
  }

  private void hideSignInRequest() {
    if (!centerPanel.isShowing()) {
      centerPanelWrapper.removeAll();
      centerPanelWrapper.add(centerPanel);
      Optional.ofNullable(getDialogButton(refreshAction)).ifPresent(b -> b.setVisible(true));
      dialogWrapper.validate();
      dialogWrapper.pack();
    }
  }

  private void validateProjectSelection() {
    if (projectListTable.getSelectedRow() >= 0) {
      dialogWrapper.setOKActionEnabled(true);
    } else {
      dialogWrapper.setOKActionEnabled(false);
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

  @VisibleForTesting
  String getSelectedProjectName() {
    // row number change based on filtering state.
    int actualSelectedRow =
        projectListTable.getRowSorter().convertRowIndexToModel(projectListTable.getSelectedRow());
    return projectListTableModel.getProjectNameAtRow(actualSelectedRow);
  }

  @VisibleForTesting
  JButton getDialogButton(@NotNull Action action) {
    return dialogWrapper.getButton(action);
  }

  @VisibleForTesting
  JComboBox<CredentialedUser> getAccountComboBox() {
    return accountComboBox;
  }

  @VisibleForTesting
  JTable getProjectListTable() {
    return projectListTable;
  }

  @VisibleForTesting
  ProjectListTableModel getProjectListTableModel() {
    return projectListTableModel;
  }

  @VisibleForTesting
  JPanel getCenterPanelWrapper() {
    return centerPanelWrapper;
  }

  @VisibleForTesting
  void setProjectLoader(ProjectLoader projectLoader) {
    this.projectLoader = projectLoader;
  }

  @VisibleForTesting
  void setDialogWrapper(ProjectSelectionDialogWrapper dialogWrapper) {
    this.dialogWrapper = dialogWrapper;
  }

  @VisibleForTesting
  void addProjectListFutureCallback(
      ListenableFuture<List<Project>> future, FutureCallback<List<Project>> callback) {
    Futures.addCallback(future, callback);
  }

  /**
   * Wraps this form as an IDEA dialog instead of inheriting dialog internals.
   */
  @VisibleForTesting
  class ProjectSelectionDialogWrapper extends DialogWrapper {

    private ProjectSelectionDialogWrapper() {
      super(false /* cannot be parent */);
      init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return centerPanelWrapper;
    }

    // IntelliJ API - creates actions (buttons) for "left side" of the dialog bottom panel.
    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
      return new Action[]{refreshAction};
    }

    @Override
    public void setOKActionEnabled(boolean isEnabled) {
      super.setOKActionEnabled(isEnabled);
    }

    @Override
    protected void setErrorInfoAll(@NotNull List<ValidationInfo> info) {
      super.setErrorInfoAll(info);
    }

    @Nullable
    @Override
    protected JButton getButton(@NotNull Action action) {
      return super.getButton(action);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
      return filterTextField;
    }
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
        // use just email if no name is set or email in "()" if set.
        setText(String
            .format("%s (%s)", Strings.nullToEmpty(user.getName()), user.getEmail()));
        setIcon(GoogleLoginIcons.getScaledUserIcon(ProjectSelector.ACCOUNT_ICON_SIZE, user));
      }
    }
  }
}