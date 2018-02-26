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

import com.google.cloud.tools.intellij.ui.CopyToClipboardActionListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.jetbrains.annotations.Nullable;

/**
 * Dialog confirming the download of the service account JSON key with information on how to set the
 * credential environment variables for local run.
 */
public class ServiceAccountKeyDisplayDialog extends DialogWrapper {

  private JLabel downloadPathLabel;
  private JPanel panel;
  private JLabel credentialEnvVarLabel;
  private JButton copyToClipboardButton;
  private JTextPane envVarInfoText;
  private static final String CREDENTIAL_ENV_VAR_KEY = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String ENV_VAR_DISPLAY_FORMAT = "%s=%s";

  ServiceAccountKeyDisplayDialog(@Nullable Project project, String downloadPath) {
    super(project);
    init();

    setTitle(GctBundle.message("cloud.apis.service.account.key.downloaded.title"));
    downloadPathLabel.setText(downloadPath);

    envVarInfoText.setBackground(panel.getBackground());

    String credentialEnvVar =
        String.format(ENV_VAR_DISPLAY_FORMAT, CREDENTIAL_ENV_VAR_KEY, downloadPath);
    credentialEnvVarLabel.setText(credentialEnvVar);

    copyToClipboardButton.addActionListener(new CopyToClipboardActionListener(credentialEnvVar));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return panel;
  }
}
