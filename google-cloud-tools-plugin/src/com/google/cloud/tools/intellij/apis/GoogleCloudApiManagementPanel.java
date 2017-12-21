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

import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import java.awt.Component;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 * Panel allowing management activity of cloud APIs including api enablement and service account
 * creation.
 */
public class GoogleCloudApiManagementPanel {

  private JPanel contentPanel;
  private ProjectSelector projectSelector;
  private JLabel selectProjectLabel;
  private JBScrollPane apisScrollPane;
  private JPanel apisPanel;
  private JPanel selectedApisPanel;
  private JPanel selectedApisWrapper;
  private JTable selectedApisTable;
  private JPanel allApisPanel;
  private JPanel allApisWrapper;
  private JTable allApisTable;

  private static final int MIN_CLOUD_API_COL_WIDTH = 300;
  private static final int MIN_CLOUD_API_ENABLEMENT_COL_WIDTH = 100;
  private static final int CLOUD_API_NAME_COL = 0;
  private static final int CLOUD_API_ENABLEMENT_COL = 1;

  GoogleCloudApiManagementPanel() {
    apisPanel.setBorder(BorderFactory.createEmptyBorder());

    selectedApisWrapper.setBorder(IdeBorderFactory.createBorder());
    selectedApisPanel.setBorder(
        IdeBorderFactory.createTitledBorder(
            GctBundle.message("cloud.apis.management.selectedapis.title")));

    allApisWrapper.setBorder(IdeBorderFactory.createBorder());
    allApisPanel.setBorder(
        IdeBorderFactory.createTitledBorder(
            GctBundle.message("cloud.apis.management.allapis.title")));

    if (projectSelector.getSelectedProject() != null) {
      showApiEnablementUi();
    }
    projectSelector.addProjectSelectionListener(project -> showApiEnablementUi());
  }

  private void showApiEnablementUi() {
    apisScrollPane.setVisible(true);
    selectProjectLabel.setVisible(false);
  }

  /** Initializes the table models of the API tables based on the user's library selection. */
  // TODO(eshaul) its very important that we add an equals method to CloudLibrary so that the set
  // operation is reliable
  void init(List<CloudLibrary> allLibraries, Set<CloudLibrary> selectedLibraries) {
    selectedApisTable.setModel(new ApisTableModel(selectedLibraries));
    selectedApisTable
        .getColumnModel()
        .getColumn(CLOUD_API_NAME_COL)
        .setMinWidth(MIN_CLOUD_API_COL_WIDTH);
    selectedApisTable
        .getColumnModel()
        .getColumn(CLOUD_API_ENABLEMENT_COL)
        .setMinWidth(MIN_CLOUD_API_ENABLEMENT_COL_WIDTH);

    allApisTable.setModel(
        new ApisTableModel(Sets.difference(Sets.newHashSet(allLibraries), selectedLibraries)));
    allApisTable
        .getColumnModel()
        .getColumn(CLOUD_API_NAME_COL)
        .setMinWidth(MIN_CLOUD_API_COL_WIDTH);
    allApisTable
        .getColumnModel()
        .getColumn(CLOUD_API_ENABLEMENT_COL)
        .setMinWidth(MIN_CLOUD_API_ENABLEMENT_COL_WIDTH);
  }

  JPanel getContentPanel() {
    return contentPanel;
  }

  @VisibleForTesting
  JBScrollPane getApisScrollPane() {
    return apisScrollPane;
  }

  @VisibleForTesting
  JLabel getSelectProjectLabel() {
    return selectProjectLabel;
  }

  @VisibleForTesting
  JTable getSelectedApisTable() {
    return selectedApisTable;
  }

  @VisibleForTesting
  JTable getAllApisTable() {
    return allApisTable;
  }

  @VisibleForTesting
  ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  private void createUIComponents() {
    selectedApisTable = new ApisTable();
    selectedApisTable.setTableHeader(null);

    allApisTable = new ApisTable();
    allApisTable.setTableHeader(null);
  }

  private static final class ApisTable extends JBTable {
    ApisTable() {
      setDefaultRenderer(CloudLibrary.class, new CloudLibraryRenderer());
      setDefaultRenderer(Boolean.class, new CloudApiEnablementRenderer());
    }
  }

  /** Custom {@link TableModel} for the API enablement table instances. */
  @VisibleForTesting
  static final class ApisTableModel implements TableModel {

    private final SortedMap<CloudLibrary, Boolean> librariesToEnabledStatus =
        new TreeMap<>(Comparator.comparing(CloudLibrary::getName));

    ApisTableModel(Collection<CloudLibrary> libraries) {
      librariesToEnabledStatus.putAll(Maps.toMap(libraries, this::getEnabledStatus));
    }

    boolean getEnabledStatus(CloudLibrary library) {
      // TODO(eshaul) query the IAM API to get the actual status of the API
      return false;
    }

    @Override
    public int getRowCount() {
      return librariesToEnabledStatus.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
      return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case CLOUD_API_NAME_COL:
          return CloudLibrary.class;
        case CLOUD_API_ENABLEMENT_COL:
          return Boolean.class;
        default:
          throw new IndexOutOfBoundsException();
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex) {
        case CLOUD_API_NAME_COL:
          return librariesToEnabledStatus.keySet().toArray()[rowIndex];
        case CLOUD_API_ENABLEMENT_COL:
          return librariesToEnabledStatus.values().toArray()[rowIndex];
        default:
          throw new IndexOutOfBoundsException();
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

    @Override
    public void addTableModelListener(TableModelListener l) {}

    @Override
    public void removeTableModelListener(TableModelListener l) {}
  }

  /**
   * Custom cell renderer to handle display of the enablement column of the API enablement table. If
   * the API is not enabled on GCP, then a button is rendered allowing the user to enable the API
   * from the IDE.
   */
  private static final class CloudApiEnablementRenderer extends DefaultTableCellRenderer {

    private final JButton enableButton =
        new JButton(GctBundle.message("cloud.apis.management.enable.button"));

    CloudApiEnablementRenderer() {
      super();

      setOpaque(false);
      enableButton.setOpaque(false);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JComponent component =
          (JComponent)
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      if (column != CLOUD_API_ENABLEMENT_COL) {
        return component;
      }

      setHorizontalAlignment(CENTER);
      boolean isEnabled;
      if (value instanceof String) {
        isEnabled = Boolean.parseBoolean((String) value);
      } else {
        isEnabled = (Boolean) value;
      }

      if (isEnabled) {

        component.setBorder(IdeBorderFactory.createEmptyBorder());
        setText(GctBundle.message("cloud.apis.management.enabled.label"));

        return component;
      } else {
        return enableButton;
      }
    }
  }
}
