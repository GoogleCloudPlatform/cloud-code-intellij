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

import com.google.api.services.iam.v1.model.Role;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.fest.util.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming GCP API management actions including API enablement and service account key
 * management.
 */
public class CloudApiManagementConfirmationDialog extends DialogWrapper {

  private JPanel panel;
  private JPanel apisToEnablePanel;
  private JPanel apisNotSelectedToEnablePanel;
  private JList<String> apisToEnableList;
  private JList<String> apisNotSelectedToEnableList;
  private JLabel enableConfirmationLabel;
  private JLabel wontEnableConfirmationLabel;
  private JPanel apiEnablementPanel;
  private JPanel serviceAccountPanel;
  private JCheckBox newServiceAccountCheckbox;
  private JPanel serviceAccountDetailsPanel;
  private JScrollPane serviceAccountDetailsPane;
  private JTable roleTable;
  private JScrollPane rolePane;
  private JPanel rolePanel;

  private final Set<Role> roles;
  private static final boolean UPDATE_SERVICE_ACCOUNT_DEFAULT = true;

  /**
   * Initializes the Cloud API management confirmation dialog.
   *
   * @param module the {@link Module} the client libraries are added to
   * @param cloudProject the {@link CloudProject} on which the API will be managed
   * @param apisToEnable the set of APIs to be enabled on GCP
   * @param apisNotToEnable the set of APIs that won't be enabled on GCP
   * @param roles the set of roles corresponding to the selected client libraries
   */
  CloudApiManagementConfirmationDialog(
      Module module,
      CloudProject cloudProject,
      Set<CloudLibrary> apisToEnable,
      Set<CloudLibrary> apisNotToEnable,
      Set<Role> roles) {
    super(module.getProject());
    this.roles = roles;

    init();
    setTitle(GctBundle.message("cloud.apis.management.dialog.title"));

    apiEnablementPanel.setBorder(
        IdeBorderFactory.createTitledBorder(
            GctBundle.message("cloud.apis.management.dialog.enablement.header")));
    serviceAccountPanel.setBorder(
        IdeBorderFactory.createTitledBorder(
            GctBundle.message("cloud.apis.management.dialog.serviceaccount.header")));

    serviceAccountDetailsPane.setBorder(JBUI.Borders.empty());

    enableConfirmationLabel.setText(
        GctBundle.message(
            "cloud.apis.management.dialog.apistoenable.header", cloudProject.projectName()));
    wontEnableConfirmationLabel.setText(
        GctBundle.message("cloud.apis.management.dialog.apisnottoenable.header", module.getName()));

    apisToEnablePanel.setVisible(!apisToEnable.isEmpty());
    apisNotSelectedToEnablePanel.setVisible(!apisNotToEnable.isEmpty());

    populateLibraryList(apisToEnableList, apisToEnable);
    populateLibraryList(apisNotSelectedToEnableList, apisNotToEnable);

    newServiceAccountCheckbox.addActionListener(newServiceAccountClickHandler());
    newServiceAccountCheckbox.setSelected(UPDATE_SERVICE_ACCOUNT_DEFAULT);
    serviceAccountDetailsPanel.setVisible(newServiceAccountCheckbox.isSelected());
    roleTable.setTableHeader(null);
    rolePane.setBorder(JBUI.Borders.empty());
    rolePanel.setVisible(!roles.isEmpty());
  }

  Set<Role> getSelectedRoles() {
    return ((ServiceAccountRolesTableModel) roleTable.getModel()).getSelectedRoles();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }

  private ActionListener newServiceAccountClickHandler() {
    return e -> serviceAccountDetailsPane.setVisible(((JCheckBox) e.getSource()).isSelected());
  }

  private void populateLibraryList(JList<String> list, Set<CloudLibrary> libraries) {
    DefaultListModel<String> listModel = new DefaultListModel<>();
    libraries.forEach(library -> listModel.addElement(library.getName()));
    list.setModel(listModel);
  }

  private void createUIComponents() {
    roleTable = new ServiceAccountRolesTable(roles);
  }

  private static final class ServiceAccountRolesTable extends JBTable {
    ServiceAccountRolesTable(Set<Role> roles) {
      super(new ServiceAccountRolesTableModel(roles));
      setDefaultRenderer(Role.class, new RoleNameRenderer());
      setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
      setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
      TableUtil.setupCheckboxColumn(getColumnModel().getColumn(1));
      setBorder(IdeBorderFactory.createBorder());
      setRowHeight(25);
    }

    /** See {@link com.intellij.ide.plugins.PluginTable#paint(Graphics)} for reasoning. */
    @Override
    public void paint(@NotNull Graphics g) {
      super.paint(g);
      UIUtil.fixOSXEditorBackground(this);
    }
  }

  private static final class RoleNameRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object role, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, role, isSelected, hasFocus, row, column);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      setText(((Role) role).getTitle());
      return this;
    }
  }

  private static final class ServiceAccountRolesTableModel extends AbstractTableModel {

    private final SortedMap<Role, Boolean> roleMap =
        new TreeMap<>(Comparator.comparing(Role::getName));
    private static final int ROLE_NAME_COL_INDEX = 0;
    private static final int ROLE_ENABLED_COL_INDEX = 1;

    ServiceAccountRolesTableModel(Set<Role> roles) {
      roleMap.putAll(Maps.toMap(roles, role -> true));
    }

    Set<Role> getSelectedRoles() {
      return roleMap
          .entrySet()
          .stream()
          .filter(Entry::getValue)
          .map(Entry::getKey)
          .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public int getRowCount() {
      return roleMap.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnIndex == ROLE_NAME_COL_INDEX ? Role.class : Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == ROLE_ENABLED_COL_INDEX;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == ROLE_NAME_COL_INDEX) {
        return roleMap.keySet().toArray()[rowIndex];
      }
      return roleMap.values().toArray()[rowIndex];
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      Role role = (Role) roleMap.keySet().toArray()[rowIndex];
      roleMap.put(role, (boolean) value);
    }
  }

  @VisibleForTesting
  public JPanel getApisToEnablePanel() {
    return apisToEnablePanel;
  }

  @VisibleForTesting
  public JPanel getApisNotSelectedToEnablePanel() {
    return apisNotSelectedToEnablePanel;
  }

  public JPanel getRolePanel() {
    return rolePanel;
  }

  @VisibleForTesting
  public JTable getRoleTable() {
    return roleTable;
  }
}
