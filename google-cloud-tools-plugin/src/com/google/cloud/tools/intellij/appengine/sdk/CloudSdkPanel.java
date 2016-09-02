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
import com.google.cloud.tools.appengine.api.exceptions.AppEngineException;
import com.google.cloud.tools.appengine.api.exceptions.BadSdkLocationException;
import com.google.cloud.tools.appengine.api.exceptions.NotAFileException;
import com.google.cloud.tools.appengine.api.exceptions.NullSdkPathException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons.RunConfigurations;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

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

  public CloudSdkPanel() {
    warningMessage.setVisible(false);
    warningMessage.setIcon(RunConfigurations.ConfigurationWarning);

    cloudSdkDirectoryField.addBrowseFolderListener(
        GctBundle.message("appengine.cloudsdk.location.browse.window.title"),
        null /**description*/,
        null /**project*/,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
    );

    cloudSdkDirectoryField.getTextField().getDocument()
        .addDocumentListener(new DocumentAdapter() {
          @Override
          protected void textChanged(DocumentEvent event) {
            checkSdk();
          }
        });

    checkSdk();
  }

  private void checkSdk() {
    String path = cloudSdkDirectoryField.getText();

    if (StringUtil.isEmpty(path)) {
      warningMessage.setVisible(true);
      warningMessage.setText(
          GctBundle.message("appengine.cloudsdk.location.missing.message"));
      return;
    }

    try {
      new CloudSdk.Builder()
          .sdkPath(Paths.get(path))
          .build()
          .validate();

      cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
      warningMessage.setVisible(false);
    } catch (AppEngineException aee) {
      cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
      warningMessage.setVisible(true);
      if (aee instanceof NullSdkPathException) {
        warningMessage.setText(GctBundle.message("appengine.cloudsdk.location.missing.message"));
      } else if (aee instanceof BadSdkLocationException) {
        warningMessage.setText(GctBundle.message("appengine.cloudsdk.location.directory.invalid"));
      } else if (aee instanceof NotAFileException) {
        warningMessage.setText(GctBundle.message("appengine.cloudsdk.location.invalid.message"));
      }
    }
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
    CloudSdkService sdkService = CloudSdkService.getInstance();

    return !Paths.get(getCloudSdkDirectory() != null ? getCloudSdkDirectory() : "")
        .equals(sdkService.getSdkHomePath() != null ? sdkService.getSdkHomePath() : Paths.get(""));
  }

  public void apply() throws ConfigurationException {
    CloudSdkService.getInstance().setSdkHomePath(getCloudSdkDirectory());
  }

  public void reset() {
    CloudSdkService sdkService = CloudSdkService.getInstance();

    setCloudSdkDirectoryText(sdkService.getSdkHomePath() != null
        ? sdkService.getSdkHomePath().toString() : "");
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
}
