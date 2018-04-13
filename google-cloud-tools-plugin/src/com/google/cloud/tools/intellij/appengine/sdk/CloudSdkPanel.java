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

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.intellij.icons.AllIcons.RunConfigurations;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.UserActivityWatcher;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;

/** Reusable panel for configuring the path to the Cloud SDK from various contexts. */
@SuppressWarnings("FutureReturnValueIgnored")
public class CloudSdkPanel {

  private TextFieldWithBrowseButton cloudSdkDirectoryField;
  private JTextPane warningMessage;
  private JPanel cloudSdkPanel;
  private JLabel warningIcon;
  private JRadioButton managedRadioButton;
  private JRadioButton customRadioButton;
  private JCheckBox enableAutomaticUpdatesCheckbox;
  private HyperlinkLabel checkForUpdatesHyperlink;
  private JPanel managedSdkComponentsPanel;

  private static final String CLOUD_SDK_DOWNLOAD_LINK =
      "https://cloud.google.com/sdk/docs/"
          + "#install_the_latest_cloud_tools_version_cloudsdk_current_version";

  private boolean settingsModified;

  private CloudSdkServiceType selectedCloudSdkServiceType;

  public CloudSdkPanel() {
    warningMessage.setVisible(false);
    warningMessage.setBackground(cloudSdkPanel.getBackground());
    warningMessage.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    warningIcon.setVisible(false);
    warningIcon.setIcon(RunConfigurations.ConfigurationWarning);

    checkManagedSdkFeatureStatus();

    initEvents();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void checkSdkInBackground() {
    final String path = cloudSdkDirectoryField.getText();
    ApplicationManager.getApplication().executeOnPooledThread(new CloudSdkCheckerRunnable(path));
  }

  @VisibleForTesting
  protected void checkSdk(String path) {
    String message = buildSdkMessage(path, true /*htmlEnabled*/);

    if (!StringUtil.isEmpty(message)) {
      showWarning(message);
    } else {
      hideWarning();
    }
  }

  public static String buildSdkMessage(String path, boolean htmlEnabled) {
    if (StringUtil.isEmpty(path)) {
      String missingMessage = GctBundle.message("cloudsdk.location.missing.message");

      return htmlEnabled ? missingMessage + " " + getCloudSdkDownloadMessage() : missingMessage;
    }

    // Use a sorted set to guarantee consistent ordering of CloudSdkValidationResults.
    Set<CloudSdkValidationResult> validationResults =
        new TreeSet<>(CloudSdkValidator.getInstance().validateCloudSdk(path));

    if (!validationResults.isEmpty()) {
      // Display all validation results as a list.
      StringBuilder builder = new StringBuilder();

      boolean isFirst = true;
      for (CloudSdkValidationResult validationResult : validationResults) {

        String message;
        if (validationResult == CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND) {
          // If the cloud sdk is not found, provide a download URL.
          message =
              htmlEnabled
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

  @VisibleForTesting
  protected void showWarning(final String message) {
    invokePanelValidationUpdate(
        new Runnable() {
          @Override
          public void run() {
            warningMessage.setText(message);
            warningMessage.setVisible(true);
            warningIcon.setVisible(true);
            cloudSdkDirectoryField.getTextField().setForeground(JBColor.red);
          }
        });
  }

  @VisibleForTesting
  protected void hideWarning() {
    invokePanelValidationUpdate(
        () -> {
          cloudSdkDirectoryField.getTextField().setForeground(JBColor.black);
          warningIcon.setVisible(false);
          warningMessage.setVisible(false);
        });
  }

  private void invokePanelValidationUpdate(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
  }

  @VisibleForTesting
  TextFieldWithBrowseButton getCloudSdkDirectoryField() {
    return cloudSdkDirectoryField;
  }

  @VisibleForTesting
  JRadioButton getManagedRadioButton() {
    return managedRadioButton;
  }

  @VisibleForTesting
  JCheckBox getEnableAutomaticUpdatesCheckbox() {
    return enableAutomaticUpdatesCheckbox;
  }

  @VisibleForTesting
  public HyperlinkLabel getCheckForUpdatesHyperlink() {
    return checkForUpdatesHyperlink;
  }

  @VisibleForTesting
  JRadioButton getCustomRadioButton() {
    return customRadioButton;
  }

  public boolean isModified() {
    return settingsModified;
  }

  public void apply() throws ConfigurationException {
    CloudSdkServiceUserSettings sdkServiceUserSettings = CloudSdkServiceUserSettings.getInstance();

    if (customRadioButton.isSelected()) {
      String customSdkPathText = getCloudSdkDirectoryText();
      if (CloudSdkValidator.getInstance()
          .validateCloudSdk(customSdkPathText)
          .contains(CloudSdkValidationResult.MALFORMED_PATH)) {
        throw new ConfigurationException(
            GctBundle.message("appengine.cloudsdk.location.badchars.message"));
      }

      sdkServiceUserSettings.setCustomSdkPath(customSdkPathText);
    }

    CloudSdkServiceType previousSdkType = sdkServiceUserSettings.getUserSelectedSdkServiceType();
    if (previousSdkType != selectedCloudSdkServiceType) {
      // notify SDK manager about changed selection
      ServiceManager.getService(CloudSdkServiceManager.class)
          .onNewCloudSdkServiceTypeSelected(selectedCloudSdkServiceType);
    }
    sdkServiceUserSettings.setUserSelectedSdkServiceType(selectedCloudSdkServiceType);

    boolean previousAutomaticUpdateEnabled = sdkServiceUserSettings.isAutomaticUpdateEnabled();
    sdkServiceUserSettings.setEnableAutomaticUpdates(enableAutomaticUpdatesCheckbox.isSelected());
    if (enableAutomaticUpdatesCheckbox.isSelected() && !previousAutomaticUpdateEnabled) {
      // activate updates again.
      ManagedCloudSdkUpdateService.getInstance().activate();
    }

    // settings are applied and saved, clear modification status
    settingsModified = false;
  }

  public void reset() {
    CloudSdkServiceUserSettings sdkServiceUserSettings = CloudSdkServiceUserSettings.getInstance();

    CloudSdkServiceType selectedSdkServiceType =
        sdkServiceUserSettings.getUserSelectedSdkServiceType();
    switch (selectedSdkServiceType) {
      case MANAGED_SDK:
        managedRadioButton.doClick();
        break;
      case CUSTOM_SDK:
        customRadioButton.doClick();
        break;
    }

    setCloudSdkDirectoryText(Strings.nullToEmpty(sdkServiceUserSettings.getCustomSdkPath()));

    enableAutomaticUpdatesCheckbox.setSelected(sdkServiceUserSettings.isAutomaticUpdateEnabled());

    // reset modified flag too so user won't see this as changed state.
    settingsModified = false;
  }

  public String getCloudSdkDirectoryText() {
    return cloudSdkDirectoryField.getText();
  }

  /**
   * Sets the Cloud SDK directory text. Performs no action if the current value of the Cloud SDK
   * directory field is already equal to the path.
   */
  public void setCloudSdkDirectoryText(String path) {
    if (!Objects.equals(cloudSdkDirectoryField.getText(), path)) {
      cloudSdkDirectoryField.setText(path);
    }
  }

  @NotNull
  public JPanel getComponent() {
    return cloudSdkPanel;
  }

  private static String getCloudSdkDownloadMessage() {
    String openTag = "<a href='" + CLOUD_SDK_DOWNLOAD_LINK + "'>";
    return GctBundle.message("cloudsdk.download.message", openTag, "</a>");
  }

  private void checkManagedSdkFeatureStatus() {
    if (!ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.MANAGED_SDK)) {
      managedSdkComponentsPanel.setVisible(false);
      managedRadioButton.setVisible(false);
      customRadioButton.setSelected(true);
      // more specific title for this case as this panel will be re-used in multiple places.
      customRadioButton.setText(
          GctBundle.getString("cloudsdk.customsdk.without.managedsdk.feature"));
    }
  }

  private void createUIComponents() {
    checkForUpdatesHyperlink = new HyperlinkLabel();
    checkForUpdatesHyperlink.setHyperlinkText(
        GctBundle.getString("cloudsdk.check.for.updates.action"));
  }

  private void initEvents() {
    // track all changes in UI to report settings changes.
    UserActivityWatcher activityWatcher = new UserActivityWatcher();
    activityWatcher.register(cloudSdkPanel);
    activityWatcher.addUserActivityListener(() -> settingsModified = true);

    ButtonGroup sdkChoiceGroup = new ButtonGroup();
    sdkChoiceGroup.add(managedRadioButton);
    sdkChoiceGroup.add(customRadioButton);

    managedRadioButton.addActionListener(
        (e) -> {
          setManagedSdkUiAvailable(true);
          setCustomSdkUiAvailable(false);

          selectedCloudSdkServiceType = CloudSdkServiceType.MANAGED_SDK;
        });

    customRadioButton.addActionListener(
        (e) -> {
          setCustomSdkUiAvailable(true);
          setManagedSdkUiAvailable(false);

          selectedCloudSdkServiceType = CloudSdkServiceType.CUSTOM_SDK;
        });

    checkForUpdatesHyperlink.addHyperlinkListener(
        new HyperlinkAdapter() {
          @Override
          protected void hyperlinkActivated(HyperlinkEvent e) {
            CloudSdkService cloudSdkService = CloudSdkService.getInstance();
            if (cloudSdkService instanceof ManagedCloudSdkService) {
              ((ManagedCloudSdkService) cloudSdkService).update();
              // do update call once and disable for visual feedback,
              // since the following calls will essentially do nothing until update is complete.
              checkForUpdatesHyperlink.setVisible(false);
            }
          }
        });

    cloudSdkDirectoryField.addBrowseFolderListener(
        GctBundle.message("cloudsdk.location.browse.window.title"),
        null, /* description */
        null, /* project */
        FileChooserDescriptorFactory.createSingleFolderDescriptor());

    cloudSdkDirectoryField
        .getTextField()
        .getDocument()
        .addDocumentListener(
            new DocumentAdapter() {
              @Override
              protected void textChanged(DocumentEvent event) {
                checkSdkInBackground();
              }
            });
  }

  private void setManagedSdkUiAvailable(boolean available) {
    if (ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.MANAGED_SDK)) {
      enableAutomaticUpdatesCheckbox.setEnabled(available);
      // only make it visible if managed SDK is active, not currently installing or updating, and
      // not
      // up-to-date.
      CloudSdkService cloudSdkService = CloudSdkService.getInstance();
      if (cloudSdkService instanceof ManagedCloudSdkService
          && available
          && cloudSdkService.getStatus() == SdkStatus.READY) {
        if (!((ManagedCloudSdkService) cloudSdkService).isUpToDate()) {
          checkForUpdatesHyperlink.setVisible(true);
        }
      } else {
        checkForUpdatesHyperlink.setVisible(false);
      }
    }
  }

  private void setCustomSdkUiAvailable(boolean available) {
    cloudSdkDirectoryField.setEnabled(available);
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
