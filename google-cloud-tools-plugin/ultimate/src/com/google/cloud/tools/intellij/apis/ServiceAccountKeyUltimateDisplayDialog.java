/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.intellij.appengine.server.run.AppEngineServerConfigurationType;
import com.google.cloud.tools.intellij.ui.BooleanTableModel;
import com.intellij.CommonBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Created by nbashirbello on 4/2/18. */
public class ServiceAccountKeyUltimateDisplayDialog extends DialogWrapper {

  private final Project project;
  private final String gcpProjectId;
  private final String downloadPath;
  private JPanel panel1;
  private ServiceAccountKeyDownloadedPanel commonPanel;
  private JTable table;

  private BooleanTableModel tableModel;

  public ServiceAccountKeyUltimateDisplayDialog(
      @Nullable Project project, @NotNull String gcpProjectId, @NotNull String downloadPath) {
    super(project);
    this.project = project;
    this.gcpProjectId = gcpProjectId;
    this.downloadPath = downloadPath;
    init();

    getAppEngineStandardRunConfigs();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel1;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), new ApplyAction()};
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(project, gcpProjectId, downloadPath);
    List<RunConfiguration> servers = getAppEngineStandardRunConfigs();
    tableModel = new BooleanTableModel<>(servers,
        Comparator.comparing(RunConfiguration::getName), true);
    table = new ServerTable(tableModel);
  }

  private void getSelectedConfigs() {

  }

  private void updateConfigsWithEnvVars(List<RunConfiguration> configurations) {

  }

  private void updateConfigWithEnvVars(ApplicationConfiguration configuration) {
    /**
    CommonModel commonModel = (CommonModel)configuration;
    AppEngineServerModel serverModel = (AppEngineServerModel) commonModel.getServerModel();
    Map<String, String> newValues = commonPanel.getEnvironmentVariables();
    Map<String, String> oldEnvVars = serverModel.getEnvironment();
    if (oldEnvVars == null) {
      oldEnvVars = new HashMap<>();
    }
    oldEnvVars.putAll(newValues);
    serverModel.setEnvironment(oldEnvVars);
     **/

    configuration.getConfigurationEditor();
    Map<String, String> oldData = configuration.getEnvs();
    if (oldData == null) {
      oldData = new HashMap<>();
    }

    Map<String, String> newValues = new HashMap<>();//commonPanel.getEnvironmentVariables();
    oldData.putAll(newValues);
    configuration.setEnvs(oldData);
  }

  /** Adds the environment variables to the selected App Engine run configurations. */
  private class ApplyAction extends DialogWrapperAction {
    private ApplyAction() {
      super(CommonBundle.getApplyButtonText());
    }

    @Override
    protected void doAction(ActionEvent e) {
      Set<Objects> selectedItems = tableModel.getSelectedItems();
      for(Object anObject : selectedItems){
        updateConfigWithEnvVars((ApplicationConfiguration) anObject);
      }
    }
  }

  private List<RunConfiguration> getAppEngineStandardRunConfigs(){
    List<RunConfiguration> list =
        RunManager.getInstance(project)
            .getConfigurationsList(AppEngineServerConfigurationType.getInstance());

    return list;
  }

  /**
   * The custom {@link JBTable} for the table of existing Google App Engine run configurations.
   */
  private static final class ServerTable extends JBTable {

    ServerTable(BooleanTableModel tableModel) {
      super(tableModel);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setDefaultRenderer(RunConfiguration.class, new RunConfigurationRenderer());
      setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
      setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
      TableUtil.setupCheckboxColumn(getColumnModel().getColumn(1));  // TODO: replace  1
    }

    /**
     * See {@link com.intellij.ide.plugins.PluginTable#paint(Graphics)} for reasoning.
     */
    @Override
    public void paint(@NotNull Graphics g) {
      super.paint(g);
      UIUtil.fixOSXEditorBackground(this);
    }

  }


  /**
   * The custom {@link javax.swing.table.TableCellRenderer TableCellRenderer} for {@link
   * RunConfiguration} objects.
   */
  private static final class RunConfigurationRenderer extends DefaultTableCellRenderer {

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
      if (value instanceof RunConfiguration) {
        RunConfiguration runnerAndConfigurationSettings = (RunConfiguration) value;
        setText(runnerAndConfigurationSettings.getName());
      } else {
        setText("");
      }
    }
  }
}
