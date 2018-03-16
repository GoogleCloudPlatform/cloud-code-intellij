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

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.application.options.ModulesComboBox;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.TableUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import org.eclipse.aether.version.Version;
import org.jetbrains.annotations.NotNull;

/** The form-bound class for the Cloud API selector panel. */
final class GoogleCloudApiSelectorPanel {

  private JPanel panel;
  private JBScrollPane leftScrollPane;
  private JBScrollPane rightScrollPane;
  private JBSplitter splitter;
  private GoogleCloudApiDetailsPanel detailsPanel;
  private JTable cloudLibrariesTable;
  private JLabel modulesLabel;
  private ModulesComboBox modulesComboBox;
  private ProjectSelector projectSelector;
  private JComboBox<Version> bomComboBox;
  private JLabel bomSelectorLabel;

  private final Map<CloudLibrary, CloudApiManagementSpec> apiManagementMap;
  private final List<CloudLibrary> libraries;

  private final Project project;

  private static final boolean SHOULD_ENABLE_API_DEFAULT = true;
  private static int CLOUD_LIBRARY_COL = 0;
  private static int CLOUD_LIBRARY_SELECT_COL = 1;

  private static final int NUM_BOM_VERSIONS_TO_SHOW = 5;

  GoogleCloudApiSelectorPanel(List<CloudLibrary> libraries, Project project) {
    this.libraries = libraries;
    this.project = project;

    apiManagementMap =
        libraries
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    lib -> new CloudApiManagementSpec(SHOULD_ENABLE_API_DEFAULT)));

    panel.setPreferredSize(new Dimension(800, 600));

    projectSelector.loadActiveCloudProject();

    if (ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.BOM)) {
      populateBomVersions();
      bomComboBox.addActionListener(
          event -> {
            if (cloudLibrariesTable.getSelectedRow() != -1) {
              detailsPanel.updateManagedLibraryVersion(bomComboBox.getSelectedItem().toString());
            }
          });
    } else {
      hideBomUI();
    }
  }

  /** Returns the {@link JPanel} that holds the UI elements in this panel. */
  public JPanel getPanel() {
    return panel;
  }

  /** Adds the given {@link ActionListener} to the {@link ModulesComboBox}. */
  void addModuleSelectionListener(ActionListener listener) {
    modulesComboBox.addActionListener(listener);
  }

  /** Adds the given {@link TableModelListener} to the {@link TableModel}. */
  void addTableModelListener(TableModelListener listener) {
    cloudLibrariesTable.getModel().addTableModelListener(listener);
  }

  /** Returns the selected {@link Module}. */
  Module getSelectedModule() {
    return modulesComboBox.getSelectedModule();
  }

  /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
  Set<CloudLibrary> getSelectedLibraries() {
    return ((CloudLibraryTableModel) cloudLibrariesTable.getModel()).getSelectedLibraries();
  }

  CloudProject getCloudProject() {
    return projectSelector.getSelectedProject();
  }

  Set<CloudLibrary> getApisToEnable() {
    return getSelectedLibraries()
        .stream()
        .filter(library -> Objects.nonNull(library.getServiceName()))
        .filter(library -> apiManagementMap.get(library).shouldEnable())
        .collect(Collectors.toSet());
  }

  /** Returns the {@link ModulesComboBox} in this panel. */
  @VisibleForTesting
  ModulesComboBox getModulesComboBox() {
    return modulesComboBox;
  }

  /**
   * Returns the {@link JTable} that holds the list of available {@link CloudLibrary
   * CloudLibraries}.
   */
  @VisibleForTesting
  JTable getCloudLibrariesTable() {
    return cloudLibrariesTable;
  }

  /** Returns the {@link GoogleCloudApiDetailsPanel} that shows the selected library details. */
  @VisibleForTesting
  GoogleCloudApiDetailsPanel getDetailsPanel() {
    return detailsPanel;
  }

  /**
   * Returns the API management map holding the mapping from {@link CloudLibrary} to {@link
   * CloudApiManagementSpec}.
   */
  @VisibleForTesting
  Map<CloudLibrary, CloudApiManagementSpec> getApiManagementMap() {
    return apiManagementMap;
  }

  /** Returns the {@link ProjectSelector} in this panel. */
  @VisibleForTesting
  ProjectSelector getProjectSelector() {
    return projectSelector;
  }

  @VisibleForTesting
  public JLabel getBomSelectorLabel() {
    return bomSelectorLabel;
  }

  @VisibleForTesting
  public JComboBox<Version> getBomComboBox() {
    return bomComboBox;
  }

  /**
   * Initializes some UI components in this panel that require special set-up.
   *
   * <p>This is automatically called by the IDEA SDK and should not be directly invoked.
   */
  private void createUIComponents() {
    modulesComboBox = new ModulesComboBox();
    modulesComboBox.fillModules(project);

    ApplicationManager.getApplication()
        .runReadAction(
            () -> {
              Module[] modules = ModuleManager.getInstance(project).getSortedModules();
              if (modules.length > 0) {
                // Defaults to the first, top-level module in this project.
                modulesComboBox.setSelectedModule(modules[0]);
              }
            });

    cloudLibrariesTable = new CloudLibraryTable(libraries);
    cloudLibrariesTable.setTableHeader(null);
    cloudLibrariesTable
        .getSelectionModel()
        .addListSelectionListener(
            e -> {
              ListSelectionModel model = (ListSelectionModel) e.getSource();
              if (!model.isSelectionEmpty()) {
                int selectedIndex = model.getMinSelectionIndex();
                CloudLibrary library =
                    (CloudLibrary)
                        cloudLibrariesTable.getModel().getValueAt(selectedIndex, CLOUD_LIBRARY_COL);
                detailsPanel.setCloudLibrary(
                    library,
                    bomComboBox.getSelectedItem().toString(),
                    apiManagementMap.get(library));
                updateManagementUI();
              }
            });
    addTableModelListener(e -> updateManagementUI());

    projectSelector = new ProjectSelector(project);
    projectSelector.addProjectSelectionListener(cloudProject -> updateManagementUI());
  }

  private void updateManagementUI() {
    TableModel model = cloudLibrariesTable.getModel();
    boolean addLibrary =
        cloudLibrariesTable.getSelectedRow() != -1
            && (boolean)
                model.getValueAt(cloudLibrariesTable.getSelectedRow(), CLOUD_LIBRARY_SELECT_COL);
    detailsPanel.setManagementUIEnabled(addLibrary && projectSelector.getSelectedProject() != null);
  }

  /**
   * Populates the BOM {@link JComboBox} with the fetched versions. If there are no versions
   * returned, then the BOM UX is hidden.
   *
   * <p>Sorts the displayable versions in reverse order, and limits the number shown to some value
   * N.
   */
  // TODO (eshaul): make async with loader icons
  private void populateBomVersions() {
    List<Version> bomVersions =
        Lists.newArrayList(CloudApiMavenService.getInstance().getBomVersions());

    if (bomVersions.isEmpty()) {
      hideBomUI();
    } else {
      bomVersions.sort(Comparator.reverseOrder());

      if (bomVersions.size() > NUM_BOM_VERSIONS_TO_SHOW) {
        bomVersions = bomVersions.subList(0, NUM_BOM_VERSIONS_TO_SHOW);
      }

      bomVersions.forEach(bomComboBox::addItem);
    }
  }

  private void hideBomUI() {
    bomComboBox.setVisible(false);
    bomSelectorLabel.setVisible(false);
  }

  /** The custom {@link JBTable} for the table of supported Cloud libraries. */
  private static final class CloudLibraryTable extends JBTable {

    CloudLibraryTable(List<CloudLibrary> libraries) {
      super(new CloudLibraryTableModel(libraries));
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setDefaultRenderer(CloudLibrary.class, new CloudLibraryRenderer());
      setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
      setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
      TableUtil.setupCheckboxColumn(getColumnModel().getColumn(1));
    }

    /** See {@link com.intellij.ide.plugins.PluginTable#paint(Graphics)} for reasoning. */
    @Override
    public void paint(@NotNull Graphics g) {
      super.paint(g);
      UIUtil.fixOSXEditorBackground(this);
    }
  }

  /**
   * The custom {@link javax.swing.table.TableCellRenderer TableCellRenderer} for {@link
   * CloudLibrary} objects.
   */
  private static final class CloudLibraryRenderer extends DefaultTableCellRenderer {

    private static final Border NO_FOCUS_BORDER = new EmptyBorder(5, 5, 5, 5);

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component component =
          super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      setBorder(NO_FOCUS_BORDER);
      return component;
    }

    @Override
    public void setValue(Object value) {
      if (value instanceof CloudLibrary) {
        CloudLibrary library = (CloudLibrary) value;
        setText(library.getName());
      } else {
        setText("");
      }
    }
  }

  /** The {@link TableModel} for the table of supported Cloud libraries. */
  private static final class CloudLibraryTableModel implements TableModel {

    private final SortedMap<CloudLibrary, Boolean> librariesMap =
        new TreeMap<>(Comparator.comparing(CloudLibrary::getName));
    private final List<TableModelListener> listeners = new ArrayList<>();

    CloudLibraryTableModel(List<CloudLibrary> libraries) {
      librariesMap.putAll(Maps.toMap(libraries, lib -> false));
    }

    /** Returns the set of selected {@link CloudLibrary CloudLibraries}. */
    Set<CloudLibrary> getSelectedLibraries() {
      return librariesMap
          .entrySet()
          .stream()
          .filter(Entry::getValue)
          .map(Entry::getKey)
          .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public int getRowCount() {
      return librariesMap.size();
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
      if (columnIndex == CLOUD_LIBRARY_COL) {
        return CloudLibrary.class;
      }
      return Boolean.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == CLOUD_LIBRARY_SELECT_COL;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == CLOUD_LIBRARY_COL) {
        return librariesMap.keySet().toArray()[rowIndex];
      }
      return librariesMap.values().toArray()[rowIndex];
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if (columnIndex == CLOUD_LIBRARY_COL) {
        throw new UnsupportedOperationException("The first column is immutable.");
      }

      CloudLibrary key = (CloudLibrary) librariesMap.keySet().toArray()[rowIndex];
      librariesMap.put(key, (Boolean) value);

      TableModelEvent event = new TableModelEvent(this, rowIndex);
      listeners.forEach(listener -> listener.tableChanged(event));
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
      listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
      listeners.remove(l);
    }
  }
}
