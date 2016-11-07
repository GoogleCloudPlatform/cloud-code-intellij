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

import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.icons.AllIcons.RunConfigurations;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;

/**
 * Reusable panel for configuring the path to the Cloud SDK from various contexts.
 */
public class CloudSdkPanel {

  private TextFieldWithBrowseButton cloudSdkDirectoryField;
  private JTextPane warningMessage;
  private JPanel cloudSdkPanel;
  private JLabel warningIcon;

  private static final String CLOUD_SDK_DOWNLOAD_LINK = "https://cloud.google.com/sdk/docs/"
      + "#install_the_latest_cloud_tools_version_cloudsdk_current_version";

  public CloudSdkPanel() {
    warningMessage.setVisible(false);
    warningMessage.setBackground(cloudSdkPanel.getBackground());
    warningMessage.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    warningIcon.setVisible(false);
    warningIcon.setIcon(RunConfigurations.ConfigurationWarning);

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
      String warningText = appendCloudSdkDownloadMessage(
              GctBundle.message("appengine.cloudsdk.location.missing.message"));
      showWarning(warningText, false /* setSdkDirectoryErrorState */);
      return;
    }

    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk(Paths.get(path));

    // if there are any validation errors, show the first one to the user
    if (validationResults.size() > 0) {
      CloudSdkValidationResult validationResult = validationResults.iterator().next();
      String message;
      switch (validationResult) {
        case CLOUD_SDK_NOT_FOUND:
          message = appendCloudSdkDownloadMessage(validationResult.getMessage());
          break;
        case CLOUD_SDK_VERSION_NOT_SUPPORTED:
          message = validationResult.getMessage();
          break;
        default:
          message = validationResult.getMessage();
      }
      showWarning(message, validationResult.isError());
    } else {
      hideWarning();
    }
  }

  private void showWarning(String message, boolean setSdkDirectoryErrorState) {
    warningIcon.setVisible(true);
    warningMessage.setVisible(true);
    warningMessage.setText(message);

    if (setSdkDirectoryErrorState) {
      cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
    }
  }

  private void hideWarning() {
    cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
    warningIcon.setVisible(false);
    warningIcon.setVisible(false);
    warningMessage.setVisible(false);
  }

  @VisibleForTesting
  public TextFieldWithBrowseButton getCloudSdkDirectoryField() {
    return cloudSdkDirectoryField;
  }

  @VisibleForTesting
  JTextPane getWarningMessage() {
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

  private String appendCloudSdkDownloadMessage(String message) {
    String openTag = "<a href='"
        + CLOUD_SDK_DOWNLOAD_LINK
        + "'>";

    return message + " "
        + GctBundle.message("appengine.cloudsdk.download.message", openTag, "</a>");
  }

}
