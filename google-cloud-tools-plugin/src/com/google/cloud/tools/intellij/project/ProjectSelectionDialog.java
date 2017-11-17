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

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import git4idea.DialogManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectSelectionDialog extends DialogWrapper {

  private JComboBox accountComboBox;
  private JButton addAccountButton;
  private JTextField filterTextField;
  private JTable projectListTable;
  private JPanel centerPanel;

  private ProjectSelection projectSelection;
  private ProjectListTableModel projectListTableModel;

  private ProjectSelectionDialog(Component parent) {
    super(parent, false);
    init();
  }

  static ProjectSelection showDialog(Component parent, ProjectSelection projectSelection) {
    ProjectSelectionDialog selectionDialog = new ProjectSelectionDialog(parent);
    selectionDialog.setProjectSelection(projectSelection);
    DialogManager.show(selectionDialog);
    return selectionDialog.projectSelection;
  }

  void setProjectSelection(ProjectSelection projectSelection) {
    this.projectSelection = projectSelection;
    updateProjectAccountInformation();
  }

  @Override
  protected void init() {
    super.init();
    setTitle("Select Google Cloud Project");
  }

  @NotNull
  @Override
  protected Action[] createLeftSideActions() {
    return new Action[] {new RefreshAction()};
  }

  private void createUIComponents() {
    // prepare account combobox model and rendering.

    // prepare table model and rendering.
    projectListTable = new JBTable();
    projectListTableModel = new ProjectListTableModel();
    projectListTable.setModel(projectListTableModel);
  }

  private void updateProjectAccountInformation() {
    if (projectSelection != null) {
      accountComboBox.addItem(projectSelection.getUser().getName());
      filterTextField.setText(projectSelection.getProject().getName());
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
          return "Project Name";
        case 1:
          return "Project ID";
      }

      return "";
    }
  }
}
