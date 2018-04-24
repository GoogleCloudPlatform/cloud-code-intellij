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

package com.google.cloud.tools.intellij.appengine.java.ultimate.apis;

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.apis.ServiceAccountKeyDownloadedPanel;
import com.google.cloud.tools.intellij.appengine.java.ultimate.server.run.AppEngineServerConfigurationType;
import com.google.cloud.tools.intellij.project.CloudProject;
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
public class ServiceAccountKeyUltimateDisplayDialog extends DialogWrapper {
  private final Project project;
  private final CloudProject cloudProject;
  private final String downloadPath;
  private JPanel mainPanel;
  private ServiceAccountKeyDownloadedPanel commonPanel;
  private JTable runConfigurationTable;
  private JLabel runConfigurationUpdateLabel;
  private JScrollPane scrollPane;
  private BooleanTableModel<RunnerAndConfigurationSettings> runConfigurationTableModel;
  private AddVariablesAction addVariablesAction;

  @VisibleForTesting
  public static List<RunnerAndConfigurationSettings> configurationSettingsList;

  ServiceAccountKeyUltimateDisplayDialog(
      @Nullable Project project, @NotNull CloudProject cloudProject, @NotNull String downloadPath) {
    super(project);
    this.project = project;
    this.cloudProject = cloudProject;
    this.downloadPath = downloadPath;
    init();
    setTitle(GctBundle.message("cloud.apis.service.account.key.downloaded.title"));
    runConfigurationTable.setTableHeader(null);

    if (runConfigurationTableModel.getRowCount() == 0) {
      runConfigurationUpdateLabel.setVisible(false);
      scrollPane.setVisible(false);
      runConfigurationTable.setVisible(false);
    }

    runConfigurationTableModel.addTableModelListener(
        e -> {
          if (addVariablesAction != null) {
            addVariablesAction.setEnabled(
                !runConfigurationTableModel.getSelectedItems().isEmpty());
          }
        });
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
    if (runConfigurationTableModel.getRowCount() > 0) {
      addVariablesAction = new AddVariablesAction();
      actions.add(addVariablesAction);
    }
    actions.add(getCancelAction());
    setCancelButtonText(CommonBundle.getCloseButtonText());
    return actions.toArray(new Action[0]);
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(cloudProject.projectId(), downloadPath);
    List<RunnerAndConfigurationSettings> configurationSettingsList =
        getAppEngineStandardConfigurationSettingsList();

    if (runConfigurationTableModel == null) {
      runConfigurationTableModel =
          new BooleanTableModel<>(
              configurationSettingsList,
              RunnerAndConfigurationSettings.class,
              Comparator.comparing(RunnerAndConfigurationSettings::getName),
              true);
    }
    runConfigurationTable = new RunConfigurationTable(runConfigurationTableModel);
  }

  private List<RunnerAndConfigurationSettings> getAppEngineStandardConfigurationSettingsList() {
    if (configurationSettingsList != null) {
      return configurationSettingsList;
    }

    return RunManager.getInstance(project)
        .getConfigurationSettingsList(AppEngineServerConfigurationType.getInstance());
  }

  private Set<RunnerAndConfigurationSettings> getSelectedConfigurations() {
    return runConfigurationTableModel.getSelectedItems();
  }

  @VisibleForTesting
  public void addEnvironmentVariablesToConfiguration(
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
      setErrorText(
          GctBundle.message(
              "cloud.apis.service.account.key.dialog.update.configuration.error",
              configuration.getName()),
          mainPanel);
      return;
    }

    RunnerSpecificLocalConfigurationBit configurationSettings =
        (RunnerSpecificLocalConfigurationBit) configuration.getConfigurationSettings(runner);
    if (configurationSettings == null) {
      setErrorText(
          GctBundle.message(
              "cloud.apis.service.account.key.dialog.update.configuration.error",
              configuration.getName()),
          mainPanel);
      return;
    }

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

  @VisibleForTesting
  public JTable getRunConfigurationTable() {
    return runConfigurationTable;
  }

  /** Adds the Cloud Library environment variables to the selected App Engine run configurations. */
  private class AddVariablesAction extends DialogWrapperAction {
    private AddVariablesAction() {
      super(GctBundle.message("cloud.apis.service.account.key.downloaded.update.server.button"));
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    protected void doAction(ActionEvent event) {
      addEnvironmentVariablesToConfiguration(getSelectedConfigurations());
      if (hasErrors(mainPanel)) {
        this.setEnabled(false);
      } else {
        close(OK_EXIT_CODE);
      }
    }
  }

  /** The custom {@link JBTable} for the table of existing Google App Engine run configurations. */
  private static final class RunConfigurationTable extends JBTable {

    RunConfigurationTable(BooleanTableModel<RunnerAndConfigurationSettings> tableModel) {
      super(tableModel);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setDefaultRenderer(
          RunnerAndConfigurationSettings.class, new RunnerAndConfigurationSettingsRenderer());
      setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
      setDefaultEditor(Boolean.class, new BooleanTableCellEditor());
      TableUtil.setupCheckboxColumn(this, tableModel.getBooleanColumn());
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
    // TODO: test JBUI.Borders.empty(5)
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
