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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.icons.AllIcons.RunConfigurations;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Reusable panel for configuring the path to the Cloud SDK from various contexts.
 */
@SuppressWarnings("FutureReturnValueIgnored")
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
            checkSdkInBackground();
          }
        });
  }

  public boolean isModified() {
    CloudSdkService sdkService = CloudSdkService.getInstance();

    String cloudSdkDirectoryFieldValue =
        getCloudSdkDirectoryText() != null ? getCloudSdkDirectoryText() : "";
    String currentCloudSdkDirectory = sdkService.getSdkHomePath() != null
        ? sdkService.getSdkHomePath().toString() : "";
    return !cloudSdkDirectoryFieldValue.equals(currentCloudSdkDirectory);
  }

  public void apply() {
    CloudSdkService.getInstance().setSdkHomePath(getCloudSdkDirectoryText());
  }

  public void reset() {
    CloudSdkService sdkService = CloudSdkService.getInstance();

    // TODO(joaomartins): Suggest Cloud SDK location (by resorting to PathResolver) if the path is
    // empty? Or create a suggest button?
    setCloudSdkDirectoryText(sdkService.getSdkHomePath() != null
        ? sdkService.getSdkHomePath().toString() : "");
  }

  public String getCloudSdkDirectoryText() {
    return cloudSdkDirectoryField.getText();
  }

  /** Sets the Cloud SDK directory text. */
  public void setCloudSdkDirectoryText(String path) {
    cloudSdkDirectoryField.setText(path);
  }

  @NotNull
  public JPanel getComponent() {
    return cloudSdkPanel;
  }

  @VisibleForTesting
  public TextFieldWithBrowseButton getCloudSdkDirectoryField() {
    return cloudSdkDirectoryField;
  }

  @VisibleForTesting
  JTextPane getWarningMessage() {
    return warningMessage;
  }

  @VisibleForTesting
  JLabel getWarningIcon() {
    return warningIcon;
  }

  private void checkSdkInBackground() {
    final String path = cloudSdkDirectoryField.getText();
    ApplicationManager.getApplication().executeOnPooledThread(() -> checkSdk(path));
  }

  private void checkSdk(String path) {
    String message = buildSdkMessage(path, true /*htmlEnabled*/);

    if (!StringUtil.isEmpty(message)) {
      showWarning(message);
    } else {
      hideWarning();
    }
  }

  private String buildSdkMessage(String path, boolean htmlEnabled) {
    if (StringUtil.isEmpty(path)) {
      String missingMessage = GctBundle.message("appengine.cloudsdk.location.missing.message");

      return htmlEnabled
          ? missingMessage + " " + getCloudSdkDownloadMessage()
          : missingMessage;
    }

    CloudSdkService sdkService = CloudSdkService.getInstance();
    // Use a sorted set to guarantee consistent ordering of CloudSdkValidationResults.
    Set<CloudSdkValidationResult> validationResults =
        new TreeSet<>(sdkService.validateCloudSdk(path));

    if (!validationResults.isEmpty()) {
      // Display all validation results as a list.
      StringBuilder builder = new StringBuilder();

      boolean isFirst = true;
      for (CloudSdkValidationResult validationResult : validationResults) {

        String message;
        if (validationResult == CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND) {
          // If the cloud sdk is not found, provide a download URL.
          message = htmlEnabled
              ? validationResult.getMessage() + " " + getCloudSdkDownloadMessage()
              : validationResult.getMessage();
        } else {
          // Otherwise, just use the existing message.
          message = validationResult.getMessage();
        }

        if (isFirst) {
          builder.append(message);
        } else {
          builder.append("<p>" + message + "</p>");
        }
        isFirst = false;
      }

      return builder.toString();
    }

    return null;
  }

  private void showWarning(String message) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              warningMessage.setText(message);
              warningMessage.setVisible(true);
              warningIcon.setVisible(true);
              cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
            },
            ModalityState.any());
  }

  private void hideWarning() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
              warningIcon.setVisible(false);
              warningMessage.setVisible(false);
            },
            ModalityState.any());
  }

  private String getCloudSdkDownloadMessage() {
    String openTag = "<a href='"
        + CLOUD_SDK_DOWNLOAD_LINK
        + "'>";
    return GctBundle.message("appengine.cloudsdk.download.message", openTag, "</a>");
  }
}
