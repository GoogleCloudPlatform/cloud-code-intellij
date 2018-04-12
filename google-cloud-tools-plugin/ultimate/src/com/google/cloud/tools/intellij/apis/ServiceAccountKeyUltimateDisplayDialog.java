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
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.CommonBundle;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.javaee.run.configuration.RunnerSpecificLocalConfigurationBit;
import com.intellij.openapi.diagnostic.Logger;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.IntStream;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog visible only in Ultimate Edition that (1) confirms the download of the service account
 * JSON key for the Google Cloud Libraries , (2) provides information on how to set the environment
 * variables for local run and (3) allows the user to select App Engine Standard run configurations
 * to automatically update with these environment variables.
 */
// TODO: Add tests
public class ServiceAccountKeyUltimateDisplayDialog extends DialogWrapper {

  private static final Logger LOG =
      Logger.getInstance(ServiceAccountKeyUltimateDisplayDialog.class);

  private final Project project;
  private final String gcpProjectId;
  private final String downloadPath;
  private JPanel mainPanel;
  private ServiceAccountKeyDownloadedPanel commonPanel;
  private JTable table;
  private JLabel label;
  private JScrollPane scrollPane;
  private BooleanTableModel tableModel;

  public ServiceAccountKeyUltimateDisplayDialog(
      @Nullable Project project, @NotNull String gcpProjectId, @NotNull String downloadPath) {
    super(project);
    this.project = project;
    this.gcpProjectId = gcpProjectId;
    this.downloadPath = downloadPath;
    init();
    setTitle(GctBundle.message("cloud.apis.service.account.key.downloaded.title"));
    table.setTableHeader(null);

    if (tableModel.getRowCount() == 0) {
      label.setVisible(false);
      scrollPane.setVisible(false);
      table.setVisible(false);
    }
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    List<Action> actions = new ArrayList<>();
    actions.add(getOKAction());
    if (tableModel.getRowCount() > 0) {
      actions.add(new ApplyAction());
    }
    return actions.toArray(new Action[0]);
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(project, gcpProjectId, downloadPath);
    List<RunnerAndConfigurationSettings> configurationSettingsList =
        getAppEngineStandardConfigurationSettingsList();
    tableModel =
        new BooleanTableModel<>(
            configurationSettingsList,
            RunnerAndConfigurationSettings.class,
            Comparator.comparing(RunnerAndConfigurationSettings::getName),
            true);
    table = new ServerTable(tableModel);
  }

  private List<RunnerAndConfigurationSettings> getAppEngineStandardConfigurationSettingsList() {
    return RunManager.getInstance(project)
        .getConfigurationSettingsList(AppEngineServerConfigurationType.getInstance());
  }

  private Set<RunnerAndConfigurationSettings> getSelectedConfigurations() {
    return tableModel.getSelectedItems();
  }

  private void addEnvironmentVariablesToConfiguration(
      Set<RunnerAndConfigurationSettings> configurations) {
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    configurations.forEach(
        configurationSettings ->
            addEnvironmentVariablesToConfiguration(executorId, configurationSettings));
  }

  /**
   * Adds the environment variables for the Google Cloud Libraries to the list of environment
   * variables for {@code configuration} if they don't exist. If they exist, it replaces them.
   */
  private void addEnvironmentVariablesToConfiguration(
      String executorId, RunnerAndConfigurationSettings configuration) {

    ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, configuration);
    if (runner == null) {
      LOG.error("Error updating run configuration with Google CLoud Library environment variables");
      return;
    }

    RunnerSpecificLocalConfigurationBit configurationSettings =
        (RunnerSpecificLocalConfigurationBit) configuration.getConfigurationSettings(runner);

    Set<EnvironmentVariable> serviceAccountEnvironmentVariables =
        commonPanel.getEnvironmentVariables();
    List<EnvironmentVariable> configurationSettingsEnvVariables =
        configurationSettings.getEnvVariables();

    serviceAccountEnvironmentVariables.forEach(
        serviceAccountEnvVar -> {
          OptionalInt indexOpt =
              IntStream.range(0, configurationSettingsEnvVariables.size())
                  .filter(
                      index ->
                          serviceAccountEnvVar
                              .getName()
                              .equals(configurationSettingsEnvVariables.get(index).getName()))
                  .findFirst();
          if (indexOpt.isPresent()) {
            configurationSettingsEnvVariables.set(indexOpt.getAsInt(), serviceAccountEnvVar);
          } else {
            configurationSettingsEnvVariables.add(serviceAccountEnvVar);
          }
        });
    configurationSettings.setEnvironmentVariables(configurationSettingsEnvVariables);
  }

  /** Adds the Cloud Library environment variables to the selected App Engine run configurations. */
  private class ApplyAction extends DialogWrapperAction {
    private ApplyAction() {
      super(CommonBundle.getApplyButtonText());
    }

    @Override
    protected void doAction(ActionEvent event) {
      addEnvironmentVariablesToConfiguration(getSelectedConfigurations());
    }
  }

  /** The custom {@link JBTable} for the table of existing Google App Engine run configurations. */
  private static final class ServerTable extends JBTable {

    ServerTable(BooleanTableModel tableModel) {
      super(tableModel);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setDefaultRenderer(
          RunnerAndConfigurationSettings.class, new RunnerAndConfigurationSettingsRenderer());
      setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
      setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
      TableUtil.setupCheckboxColumn(getColumnModel().getColumn(tableModel.getBooleanColumn()));
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
   * RunnerAndConfigurationSettings} objects.
   */
  private static final class RunnerAndConfigurationSettingsRenderer
      extends DefaultTableCellRenderer {

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
      if (value instanceof RunnerAndConfigurationSettings) {
        RunnerAndConfigurationSettings runnerAndConfigurationSettings =
            (RunnerAndConfigurationSettings) value;
        setText(runnerAndConfigurationSettings.getName());
      } else {
        setText("");
      }
    }
  }
}
