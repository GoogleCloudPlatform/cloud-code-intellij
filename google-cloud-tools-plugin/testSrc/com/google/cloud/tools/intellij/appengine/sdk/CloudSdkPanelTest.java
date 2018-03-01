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

  @Mock @TestService private CloudSdkService cloudSdkService;
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
    // enable managed SDK UI - remove when feature is rolled out.
    when(pluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(true);
    // now safe to create panel spy.
    panel = spy(new CloudSdkPanel());
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
  public void testApplyWith_invalidSdk() throws Exception {
    // apply() calls should be preceded with reset()
    panel.reset();
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    panel.getCloudSdkDirectoryField().setText("/non/empty/path");

    // No exception should be thrown on invalid sdk entry from this panel
    panel.apply();
  }

  private void setValidateCloudSdkResponse(CloudSdkValidationResult... results) {
    Set<CloudSdkValidationResult> validationResults = new HashSet<>();
    Collections.addAll(validationResults, results);
    when(cloudSdkValidator.validateCloudSdk(any(String.class))).thenReturn(validationResults);
  }
}
