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

import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.project.Project;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Panel confirming the download of the service account JSON key with information on how to set the
 * credential environment variables for local run.
 */
// TODO: This is temporary and will be removed onve the error that occurs when nesting forms from
// different modules is fixed
public final class ServiceAccountKeyDownloadedPanel {
  private static final String CLOUD_PROJECT_ENV_VAR_KEY = "GOOGLE_CLOUD_PROJECT";
  private static final String CREDENTIAL_ENV_VAR_KEY = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String ENV_VAR_DISPLAY_FORMAT = "%s=%s";

  private final String gcpProjectId;
  private final String downloadPath;

  private JPanel commonPanel;
  private JLabel yourServiceAccountKeyLabel;
  private JLabel downloadPathLabel;
  private JLabel envVarInfoText;
  private JTable envVarTable;
  private JButton copyToClipboardButton;

  public ServiceAccountKeyDownloadedPanel(
      @Nullable Project project, @NotNull String gcpProjectId, @NotNull String downloadPath) {
    downloadPathLabel.setText(downloadPath);

    DefaultTableModel tableModel =
        new DefaultTableModel() {
          @Override
          public boolean isCellEditable(int row, int column) {
            return false;
          }
        };

    String credentialEnvVar =
        String.format(ENV_VAR_DISPLAY_FORMAT, CREDENTIAL_ENV_VAR_KEY, downloadPath);
    String cloudProjectEnvVar =
        String.format(ENV_VAR_DISPLAY_FORMAT, CLOUD_PROJECT_ENV_VAR_KEY, gcpProjectId);

    tableModel.setColumnCount(1);
    tableModel.addRow(new String[] {credentialEnvVar});
    tableModel.addRow(new String[] {cloudProjectEnvVar});
    envVarTable.setModel(tableModel);
    envVarTable.setRowSelectionAllowed(false);
    envVarTable.setTableHeader(null);

    // copyToClipboardButton.addActionListener(
    // new CopyToClipboardActionListener(credentialEnvVar + "\n" + cloudProjectEnvVar));

    this.gcpProjectId = gcpProjectId;
    this.downloadPath = downloadPath;
  }

  @NotNull
  public Set<EnvironmentVariable> getEnvironmentVariables() {
    Set<EnvironmentVariable> environmentVariables = new HashSet<>();
    environmentVariables.add(
        new EnvironmentVariable(CLOUD_PROJECT_ENV_VAR_KEY, gcpProjectId, false));
    environmentVariables.add(new EnvironmentVariable(CREDENTIAL_ENV_VAR_KEY, downloadPath, false));
    return environmentVariables;
  }
}
