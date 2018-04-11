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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.base.Strings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link CloudSdkPanel}. */
public class CloudSdkPanelTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private PluginInfoService pluginInfoService;

  @Mock private CloudSdkService mockCloudSdkService;
  @Mock @TestService private ManagedCloudSdkUpdateService managedCloudSdkUpdateService;
  @Mock @TestService private CloudSdkServiceManager mockCloudSdkServiceManager;
  @Mock @TestService private CloudSdkValidator cloudSdkValidator;

  private CloudSdkPanel panel;

  private static final String CLOUD_SDK_DOWNLOAD_LINK =
      "<a href='https://cloud.google.com/sdk/docs/"
          + "#install_the_latest_cloud_tools_version_cloudsdk_current_version'>Click here</a> to "
          + "download the Cloud SDK.";
  private static final String MISSING_SDK_DIR_WARNING =
      "Cloud SDK home directory is not specified. " + CLOUD_SDK_DOWNLOAD_LINK;
  private static final String INVALID_SDK_DIR_WARNING =
      "No Cloud SDK was found in the specified directory. " + CLOUD_SDK_DOWNLOAD_LINK;

  @Before
  public void setUp() {
    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(mockCloudSdkService);
    // enable managed SDK UI - remove when feature is rolled out.
    when(pluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(true);
    // now safe to create panel spy.
    panel = spy(new CloudSdkPanel());
    // reset SDK settings on each run to clean previous settings.
    CloudSdkServiceUserSettings.reset();
  }

  @Test
  public void testCheckSdk_nullSdk() {
    when(cloudSdkValidator.isValidCloudSdk(null)).thenReturn(false);
    panel.checkSdk(null);
    verify(panel, times(1)).showWarning(eq(MISSING_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  @Test
  public void testCheckSdk_emptySdk() {
    when(cloudSdkValidator.isValidCloudSdk("")).thenReturn(false);
    panel.checkSdk("");
    verify(panel, times(1)).showWarning(eq(MISSING_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  @Test
  public void testCheckSdk_invalidSdk() {
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    when(cloudSdkValidator.isValidCloudSdk("/non/empty/path")).thenReturn(false);
    panel.checkSdk("/non/empty/path");
    verify(panel, times(1)).showWarning(eq(INVALID_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  @Test
  public void testCheckSdk_unsupportedSdk() {
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED);
    when(cloudSdkValidator.isValidCloudSdk("/non/empty/path")).thenReturn(false);
    panel.checkSdk("/non/empty/path");
    verify(panel, times(1))
        .showWarning(eq(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED.getMessage()));
    verify(panel, times(0)).hideWarning();
  }

  @Test
  public void testCheckSdk_multipleValidationResults() {
    setValidateCloudSdkResponse(
        CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED,
        CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    when(cloudSdkValidator.isValidCloudSdk("/non/empty/path")).thenReturn(false);

    String expectedMessage =
        INVALID_SDK_DIR_WARNING
            + "<p>"
            + CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED.getMessage()
            + "</p>";

    panel.checkSdk("/non/empty/path");
    verify(panel, times(1)).showWarning(eq(expectedMessage));
    verify(panel, times(0)).hideWarning();
  }

  @Test
  public void testCheckSdk_validSdk() {
    when(cloudSdkValidator.isValidCloudSdk("/non/empty/path")).thenReturn(true);
    setValidateCloudSdkResponse();
    panel.checkSdk("/non/empty/path");
    verify(panel, times(0)).showWarning(any(String.class));
    verify(panel, times(1)).hideWarning();
  }

  @Test
  public void testApplyWith_invalidSdk() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              sdkPanel.reset();
              setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
              sdkPanel.getCloudSdkDirectoryField().setText("/non/empty/path");

              // No exception should be thrown on invalid sdk entry from this panel
              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }
            });
  }

  @Test
  public void defaultSdkSettings_reset_validUiState() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              // use built-in defaults.
              sdkPanel.reset();

              verifySdkPanelStateForCurrentSettings(sdkPanel);
            });
  }

  @Test
  public void customSdkSettings_reset_validUiState() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              CloudSdkServiceUserSettings userSettings = CloudSdkServiceUserSettings.getInstance();
              userSettings.setUserSelectedSdkServiceType(CloudSdkServiceType.CUSTOM_SDK);
              userSettings.setCustomSdkPath("/home/gcloud");
              sdkPanel.reset();

              verifySdkPanelStateForCurrentSettings(sdkPanel);
            });
  }

  @Test
  public void managedSdk_choice_apply_validSettings() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              sdkPanel.reset();
              sdkPanel.getManagedRadioButton().doClick();

              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }

              verifyCloudSdkSettings(
                  CloudSdkServiceType.MANAGED_SDK,
                  CloudSdkServiceUserSettings.DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES,
                  null /* no custom path */);
            });
  }

  @Test
  public void managedSdk_choice_disableAutomaticUpdates_apply_validSettings() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              sdkPanel.reset();
              sdkPanel.getManagedRadioButton().doClick();
              sdkPanel.getEnableAutomaticUpdatesCheckbox().setSelected(false);

              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }

              verifyCloudSdkSettings(
                  CloudSdkServiceType.MANAGED_SDK,
                  false, /* no auto-updates */
                  null /* no custom path */);
            });
  }

  @Test
  public void customSdk_choice_apply_validSettings() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              sdkPanel.reset();
              sdkPanel.getCustomRadioButton().doClick();
              String customSdkPath = "/home/gcloud";
              sdkPanel.getCloudSdkDirectoryField().setText(customSdkPath);

              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }

              verifyCloudSdkSettings(
                  CloudSdkServiceType.CUSTOM_SDK,
                  CloudSdkServiceUserSettings.DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES,
                  customSdkPath);
            });
  }

  @Test
  public void changeSdkType_apply_callsChangedSdkTypeCallback() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              CloudSdkServiceUserSettings.getInstance()
                  .setUserSelectedSdkServiceType(CloudSdkServiceType.MANAGED_SDK);
              sdkPanel.reset();
              sdkPanel.getCustomRadioButton().doClick();
              String customSdkPath = "/home/gcloud";
              sdkPanel.getCloudSdkDirectoryField().setText(customSdkPath);

              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }

              verify(mockCloudSdkServiceManager)
                  .onNewCloudSdkServiceTypeSelected(CloudSdkServiceType.CUSTOM_SDK);
            });
  }

  @Test
  public void automaticUpdate_enabled_callsUpdater_activate() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // use non-spy panel as spy messes up with UI event thread field updates.
              CloudSdkPanel sdkPanel = new CloudSdkPanel();
              CloudSdkServiceUserSettings.getInstance()
                  .setUserSelectedSdkServiceType(CloudSdkServiceType.MANAGED_SDK);
              CloudSdkServiceUserSettings.getInstance().setEnableAutomaticUpdates(false);
              sdkPanel.reset();

              sdkPanel.getEnableAutomaticUpdatesCheckbox().doClick();

              try {
                sdkPanel.apply();
              } catch (ConfigurationException e) {
                throw new AssertionError(e);
              }

              verify(managedCloudSdkUpdateService).activate();
            });
  }

  private void setValidateCloudSdkResponse(CloudSdkValidationResult... results) {
    Set<CloudSdkValidationResult> validationResults = new HashSet<>();
    Collections.addAll(validationResults, results);
    when(cloudSdkValidator.validateCloudSdk(any(String.class))).thenReturn(validationResults);
  }

  private void verifyCloudSdkSettings(
      CloudSdkServiceType cloudSdkServiceType,
      boolean enableAutomaticUpdates,
      String customSdkPath) {
    CloudSdkServiceUserSettings userSettings = CloudSdkServiceUserSettings.getInstance();
    assertThat(cloudSdkServiceType).isEqualTo(userSettings.getUserSelectedSdkServiceType());
    assertThat(enableAutomaticUpdates).isEqualTo(userSettings.isAutomaticUpdateEnabled());
    assertThat(customSdkPath).isEqualTo(userSettings.getCustomSdkPath());
  }

  private void verifySdkPanelStateForCurrentSettings(CloudSdkPanel sdkPanel) {
    CloudSdkServiceUserSettings userSettings = CloudSdkServiceUserSettings.getInstance();
    switch (userSettings.getUserSelectedSdkServiceType()) {
      case CUSTOM_SDK:
        assertThat(sdkPanel.getCustomRadioButton().isSelected()).isTrue();

        assertThat(sdkPanel.getManagedRadioButton().isSelected()).isFalse();
        assertThat(sdkPanel.getEnableAutomaticUpdatesCheckbox().isEnabled()).isFalse();
        break;
      case MANAGED_SDK:
        assertThat(sdkPanel.getManagedRadioButton().isSelected()).isTrue();
        assertThat(sdkPanel.getEnableAutomaticUpdatesCheckbox().isEnabled()).isTrue();

        assertThat(sdkPanel.getCustomRadioButton().isSelected()).isFalse();
        assertThat(sdkPanel.getCloudSdkDirectoryField().isEnabled()).isFalse();
        break;
    }

    assertThat(sdkPanel.getEnableAutomaticUpdatesCheckbox().isSelected())
        .isEqualTo(userSettings.isAutomaticUpdateEnabled());

    assertThat(sdkPanel.getCloudSdkDirectoryText())
        .isEqualTo(Strings.nullToEmpty(userSettings.getCustomSdkPath()));
  }
}
