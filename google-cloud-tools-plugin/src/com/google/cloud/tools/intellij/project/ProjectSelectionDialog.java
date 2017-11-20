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

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginIcons;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.table.JBTable;
import git4idea.DialogManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
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

  private ProjectSelectionDialog(Component parent) {
    super(parent, false);
    init();
  }

  /**
   * Creates and shows modal dialog to select project/account. Blocks EDT until choice is made.
   *
   * @param parent Parent component for the dialog.
   * @param cloudProject Current project selection to populate dialog UI state.
   * @return New project selection or null if user cancels.
   */
  @Nullable
  static CloudProject showDialog(
      Component parent, @Nullable CloudProject cloudProject) {
    ProjectSelectionDialog selectionDialog = new ProjectSelectionDialog(parent);
    selectionDialog.setCloudProject(cloudProject);
    DialogManager.show(selectionDialog);
    int result = selectionDialog.getExitCode();
    return result == OK_EXIT_CODE ? selectionDialog.getCloudProject() : null;
  }

  @Override
  protected void init() {
    super.init();
    setTitle(GctBundle.getString("project.selector.dialog.title"));
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

    // prepare table model and rendering.
    projectListTable = new JBTable();
    projectListTableModel = new ProjectListTableModel();
    projectListTable.setModel(projectListTableModel);

    // disabled unless project is selected in the list.
    getOKAction().setEnabled(false);
  }

  private void setCloudProject(CloudProject cloudProject) {
    this.cloudProject = cloudProject;
    updateProjectAccountInformation();
  }

  private CloudProject getCloudProject() {
    return cloudProject;
  }

  private void updateProjectAccountInformation() {
    if (cloudProject != null) {
      accountComboBox.addItem(cloudProject.getUser());
      filterTextField.setText(cloudProject.getProject().getName());
      projectListTableModel.fireTableDataChanged();
    }
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

    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return "";
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0:
          return GctBundle.getString("project.selector.project.list.project.name.column");
        case 1:
          return GctBundle.getString("project.selector.project.list.project.id.column");
      }

      return "";
    }
  }

  private static final class AccountComboBoxRenderer
      extends ListCellRendererWrapper<CredentialedUser> {

    @Override
    public void customize(
        JList list, CredentialedUser user, int index, boolean selected, boolean hasFocus) {
      setText(user.getName() + " (" + user.getEmail() + ")");
      setIcon(
          GoogleLoginIcons.getScaledUserIcon(ProjectSelector.ACCOUNT_ICON_SIZE, user));
    }
  }
}
