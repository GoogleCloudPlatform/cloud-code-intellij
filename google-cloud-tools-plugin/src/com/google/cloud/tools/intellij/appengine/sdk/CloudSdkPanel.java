/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.SystemEnvironmentProvider;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

/**
 * Reusable panel for configuring the path to the Cloud SDK from various contexts.
 */
public class CloudSdkPanel {

  private TextFieldWithBrowseButton cloudSdkDirectoryField;
  private JLabel warningMessage;
  private JPanel cloudSdkPanel;

  public CloudSdkPanel(@NotNull CloudSdkService cloudSdkService) {
    warningMessage.setVisible(false);

    if (cloudSdkService.getCloudSdkHomePath() == null) {
      final String cloudSdkDirectoryPath
          = CloudSdkUtil.findCloudSdkDirectoryPath(SystemEnvironmentProvider.getInstance());

      if (cloudSdkDirectoryPath != null) {
        cloudSdkService.setCloudSdkHomePath(cloudSdkDirectoryPath);
        cloudSdkDirectoryField.setText(cloudSdkDirectoryPath);
      }
    }

    cloudSdkDirectoryField.addBrowseFolderListener(
        GctBundle.message("appengine.cloudsdk.location.browse.window.title"),
        null /**description*/,
        null /**project*/,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

    cloudSdkDirectoryField.getTextField().getDocument()
        .addDocumentListener(new SdkDirectoryFieldListener());
  }

  @VisibleForTesting
  public TextFieldWithBrowseButton getCloudSdkDirectoryField() {
    return cloudSdkDirectoryField;
  }

  @VisibleForTesting
  JLabel getWarningMessage() {
    return warningMessage;
  }

  public boolean isModified() {
    return !Comparing.strEqual(getCloudSdkDirectory(),
        CloudSdkService.getInstance().getCloudSdkHomePath());
  }

  public void apply() throws ConfigurationException {
    if (!StringUtil.isEmpty(getCloudSdkDirectory())
        && CloudSdkUtil.containsCloudSdkExecutable(getCloudSdkDirectory())) {
      CloudSdkService.getInstance().setCloudSdkHomePath(getCloudSdkDirectory());
    }
  }

  public void reset() {
    setCloudSdkDirectoryText(CloudSdkService.getInstance().getCloudSdkHomePath());
  }

  public String getCloudSdkDirectory() {
    return cloudSdkDirectoryField.getText();
  }

  public void setCloudSdkDirectoryText(String path) {
    cloudSdkDirectoryField.setText(path);
  }

  @NotNull
  public JPanel getComponent() {
    return cloudSdkPanel;
  }

  private class SdkDirectoryFieldListener extends DocumentAdapter {

    @Override
    protected void textChanged(DocumentEvent event) {
      String path = cloudSdkDirectoryField.getText();
      boolean isValid = CloudSdkUtil.containsCloudSdkExecutable(path);
      if (isValid) {
        cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
        warningMessage.setVisible(false);
      } else {
        cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
        warningMessage.setVisible(true);
        warningMessage.setText(
            GctBundle.message("appengine.cloudsdk.location.missing.message"));
      }
    }
  }
}
