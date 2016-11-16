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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

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
  // Windows implementation of Paths doesn't cope well with reserved characters. This regex is used
  // to ensure that Windows paths can't start or end with spaces, or contain any of the special
  // <>:?* (or double quotes) characters, besides the colon for the drive.
  public static final String WIN_PATH_REGEX = "^[a-zA-Z]:[^<>:\"?*]*[^<>:\"?*\\s]$";

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

    checkSdkInBackground();
  }

  private void checkSdkInBackground() {
    final String path = cloudSdkDirectoryField.getText();
    ApplicationManager.getApplication().executeOnPooledThread(new CloudSdkCheckerRunnable(path));
  }

  @VisibleForTesting
  protected void checkSdk(String path) {
    if (StringUtil.isEmpty(path)) {
      String warningText = GctBundle.message("appengine.cloudsdk.location.missing.message")
          + " " + getCloudSdkDownloadMessage();
      showWarning(warningText , false /* setSdkDirectoryErrorState */);
      return;
    }

    Set<CloudSdkValidationResult> validationResults =
        CloudSdkService.getInstance().validateCloudSdk(Paths.get(path));

    if (validationResults.isEmpty()) {
      hideWarning();
    } else {
      // use a sorted set to guarantee consistent ordering of CloudSdkValidationResults
      validationResults = new TreeSet<>(validationResults);

      // display all validation results as a list
      StringBuilder builder = new StringBuilder();
      boolean containsErrors = false;

      int counter = 0;
      for (CloudSdkValidationResult validationResult : validationResults) {
        if (validationResult.isError()) {
          containsErrors = true;
        }

        String message;
        if (validationResult == CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND) {
          // if the cloud sdk is not found, provide a download URL
          message = validationResult.getMessage() + " " + getCloudSdkDownloadMessage();
        } else {
          // otherwise, just use the existing message
          message = validationResult.getMessage();
        }

        if (counter == 0) {
          builder.append(message);
        } else {
          builder.append("<p>" + message + "</p>");
        }
        counter++;
      }

      showWarning(builder.toString(), containsErrors);
    }
  }

  @VisibleForTesting
  protected void showWarning(final String message, final boolean setSdkDirectoryErrorState) {
    invokePanelValidationUpdate(new Runnable() {
      @Override
      public void run() {
        warningMessage.setText(message);
        warningMessage.setVisible(true);
        warningMessage.updateUI();
        warningIcon.setVisible(true);
        if (setSdkDirectoryErrorState) {
          cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
        }
      }
    });
  }

  @VisibleForTesting
  protected void hideWarning() {
    invokePanelValidationUpdate(new Runnable() {
      @Override
      public void run() {
        cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
        warningIcon.setVisible(false);
        warningIcon.setVisible(false);
        warningMessage.setVisible(false);
        cloudSdkPanel.updateUI();
      }
    });
  }

  private void invokePanelValidationUpdate(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable,
        ModalityState.stateForComponent(cloudSdkPanel));
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

    String typedDirectory = getCloudSdkDirectory() != null ? getCloudSdkDirectory() : "";
    String savedDirectory = sdkService.getSdkHomePath() != null
        ? sdkService.getSdkHomePath().toString() : "";
    return !typedDirectory.equals(savedDirectory);
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

  private String getCloudSdkDownloadMessage() {
    String openTag = "<a href='"
        + CLOUD_SDK_DOWNLOAD_LINK
        + "'>";
    return GctBundle.message("appengine.cloudsdk.download.message", openTag, "</a>");
  }

  private class CloudSdkCheckerRunnable implements Runnable {

    private final String sdkPath;

    public CloudSdkCheckerRunnable(String sdkPath) {
      this.sdkPath = sdkPath;
    }

    @Override
    public void run() {
      checkSdk(sdkPath);
    }
  }

}
