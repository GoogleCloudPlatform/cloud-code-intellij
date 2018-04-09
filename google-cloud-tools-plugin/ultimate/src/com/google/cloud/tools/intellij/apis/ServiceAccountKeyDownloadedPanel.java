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

import com.intellij.openapi.project.Project;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: delete file after fix to error accessing a nested form from another module is fixed
/**
 * Panel confirming the download of the service account JSON key with information on how to set the
 * credential environment variables for local run.
 */
public final class ServiceAccountKeyDownloadedPanel {
  private static final String CLOUD_PROJECT_ENV_VAR_KEY = "GOOGLE_CLOUD_PROJECT";
  private static final String CREDENTIAL_ENV_VAR_KEY = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String ENV_VAR_DISPLAY_FORMAT = "%s=%s";

  private JPanel commonPanel;
  private JLabel yourServiceAccountKeyLabel;
  private JLabel downloadPathLabel;
  private JLabel envVarInfoText;
  private JTable envVarTable;
  private JButton copyToClipboardButton;

  private Map<String, String> envVarsMap = new HashMap();

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

    //TODO: refactor
    envVarsMap.put(CLOUD_PROJECT_ENV_VAR_KEY, gcpProjectId);
    envVarsMap.put(CREDENTIAL_ENV_VAR_KEY, downloadPath);


    tableModel.setColumnCount(1);
    tableModel.addRow(new String[] {credentialEnvVar});
    tableModel.addRow(new String[] {cloudProjectEnvVar});
    envVarTable.setModel(tableModel);
    envVarTable.setRowSelectionAllowed(false);

    copyToClipboardButton.addActionListener(
        new CopyToClipboardActionListener(credentialEnvVar + "\n" + cloudProjectEnvVar));
  }

  public Map<String, String> getEnvironmentVariables(){
    return envVarsMap;
  }


  // TODO: temporary fix to dependency errors
  static class CopyToClipboardActionListener implements ActionListener {
    private final String text;

    CopyToClipboardActionListener(String text) {
      this.text = text;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(new StringSelection(text), null /*owner*/);
    }
  }
}
