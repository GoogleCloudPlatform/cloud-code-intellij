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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.ui.BooleanTableModel;
import com.intellij.CommonBundle;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog that (1) confirms the download of the service account JSON key for the Google Cloud
 * Libraries , (2) provides information on how to set the environment variables for local run and
 * (3) allows the user to select set of run configurations to automatically update with these
 * environment variables. Set of configurations is defined by extension point {@link
 * ServiceAccountKeyRuntimeConfigurationProvider}.
 */
public class ServiceAccountKeyDisplayDialog extends DialogWrapper {
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

  // map of extension point specified runtime configuration providers to the list of runtime
  // configurations supplied by them.
  private Map<ServiceAccountKeyRuntimeConfigurationProvider, List<RunnerAndConfigurationSettings>>
      runtimeConfigurationProviders;

  ServiceAccountKeyDisplayDialog(
      @Nullable Project project, @NotNull CloudProject cloudProject, @NotNull String downloadPath) {
    super(project);
    this.project = project;
    this.cloudProject = cloudProject;
    this.downloadPath = downloadPath;
    init();
    setTitle(
        GoogleCloudApisMessageBundle.message("cloud.apis.service.account.key.downloaded.title"));

    initTableModel(getAllRunConfigurations());
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
    if (getAllRunConfigurations().size() > 0) {
      addVariablesAction = new AddVariablesAction();
      actions.add(addVariablesAction);
    }
    actions.add(getCancelAction());
    setCancelButtonText(CommonBundle.getCloseButtonText());
    return actions.toArray(new Action[0]);
  }

  private void createUIComponents() {
    commonPanel = new ServiceAccountKeyDownloadedPanel(cloudProject.projectId(), downloadPath);

    runConfigurationTable = new RunConfigurationTable();
    runConfigurationTable.setTableHeader(null);
  }

  private List<RunnerAndConfigurationSettings> getAllRunConfigurations() {
    if (runtimeConfigurationProviders == null) {
      // build initial map of providers to list of their configurations.
      runtimeConfigurationProviders =
          Stream.of(Extensions.getExtensions(ServiceAccountKeyRuntimeConfigurationProvider.EP_NAME))
              .collect(
                  Collectors.toMap(
                      Function.identity(),
                      configProvider ->
                          configProvider.getRunConfigurationsForServiceAccount(project)));
    }

    // return flat list of all runtime configurations for UI table model.
    return runtimeConfigurationProviders
        .values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private Set<RunnerAndConfigurationSettings> getSelectedConfigurations() {
    return runConfigurationTableModel.getSelectedItems();
  }

  @VisibleForTesting
  void initTableModel(List<RunnerAndConfigurationSettings> configurationSettingsList) {
    runConfigurationTableModel =
        new BooleanTableModel<>(
            configurationSettingsList,
            RunnerAndConfigurationSettings.class,
            Comparator.comparing(RunnerAndConfigurationSettings::getName),
            true);

    ((RunConfigurationTable) runConfigurationTable).setBooleanModel(runConfigurationTableModel);

    boolean runConfigurationDataAvailable = runConfigurationTableModel.getRowCount() != 0;
    runConfigurationUpdateLabel.setVisible(runConfigurationDataAvailable);
    scrollPane.setVisible(runConfigurationDataAvailable);
    runConfigurationTable.setVisible(runConfigurationDataAvailable);

    runConfigurationTableModel.addTableModelListener(
        e -> {
          if (addVariablesAction != null) {
            addVariablesAction.setEnabled(!runConfigurationTableModel.getSelectedItems().isEmpty());
          }
        });
  }

  /**
   * Adds the environment variables for the Google Cloud Libraries to the list of environment
   * variables for given set of configurations if they don't exist. If they exist, it replaces them.
   *
   * @return true if adding variables succeeded, false in case of any error.
   */
  private boolean addEnvironmentVariablesToConfiguration(
      Set<RunnerAndConfigurationSettings> configurations) {
    boolean result = true;
    for (RunnerAndConfigurationSettings configurationSettings : configurations) {
      for (Map.Entry<
              ServiceAccountKeyRuntimeConfigurationProvider, List<RunnerAndConfigurationSettings>>
          nextProviderEntry : runtimeConfigurationProviders.entrySet()) {
        if (nextProviderEntry.getValue().contains(configurationSettings)) {
          Optional<String> errorMessage =
              nextProviderEntry
                  .getKey()
                  .addEnvironmentVariablesToConfiguration(
                      configurationSettings, commonPanel.getEnvironmentVariables());
          if (errorMessage.isPresent()) {
            setErrorText(errorMessage.get(), mainPanel);
            result = false;
          }
        }
      }
    }

    return result;
  }

  @VisibleForTesting
  JTable getRunConfigurationTable() {
    return runConfigurationTable;
  }

  /** Adds the Cloud Library environment variables to the selected App Engine run configurations. */
  private class AddVariablesAction extends DialogWrapperAction {
    private AddVariablesAction() {
      super(
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.key.downloaded.update.server.button"));
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    protected void doAction(ActionEvent event) {
      if (addEnvironmentVariablesToConfiguration(getSelectedConfigurations())) {
        close(OK_EXIT_CODE);
      } else {
        this.setEnabled(false);
      }
    }
  }

  /** The custom {@link JBTable} for the table of existing Google App Engine run configurations. */
  private static final class RunConfigurationTable extends JBTable {

    // for unit tests.
    RunConfigurationTable() {
      super();
    }

    private void setBooleanModel(
        @NotNull BooleanTableModel<RunnerAndConfigurationSettings> tableModel) {
      setModel(tableModel);
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
    private static final Border NO_FOCUS_BORDER = JBUI.Borders.empty(5);

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
