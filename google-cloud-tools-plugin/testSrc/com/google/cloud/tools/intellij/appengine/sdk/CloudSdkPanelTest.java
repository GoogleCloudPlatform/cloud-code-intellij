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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.containers.HashSet;
import java.util.Collections;
import java.util.Set;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.picocontainer.MutablePicoContainer;

/** Tests for {@link CloudSdkPanel}. */
public class CloudSdkPanelTest extends PlatformTestCase {

  @Spy private CloudSdkPanel panel;

  private CloudSdkService cloudSdkService;

  private static final String CLOUD_SDK_DOWNLOAD_LINK =
      "<a href='https://cloud.google.com/sdk/docs/"
          + "#install_the_latest_cloud_tools_version_cloudsdk_current_version'>Click here</a> to "
          + "download the Cloud SDK.";
  private static final String MISSING_SDK_DIR_WARNING =
      "Cloud SDK home directory is not specified. " + CLOUD_SDK_DOWNLOAD_LINK;
  private static final String INVALID_SDK_DIR_WARNING =
      "No Cloud SDK was found in the specified directory. " + CLOUD_SDK_DOWNLOAD_LINK;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MockitoAnnotations.initMocks(this);

    MutablePicoContainer applicationContainer =
        (MutablePicoContainer) ApplicationManager.getApplication().getPicoContainer();

    cloudSdkService = mock(CloudSdkService.class);

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);
  }

  public void testCheckSdk_nullSdk() throws InterruptedException {
    when(cloudSdkService.isValidCloudSdk(null)).thenReturn(false);
    panel.checkSdk(null);
    verify(panel, times(1)).showWarning(eq(MISSING_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  public void testCheckSdk_emptySdk() throws InterruptedException {
    when(cloudSdkService.isValidCloudSdk("")).thenReturn(false);
    panel.checkSdk("");
    verify(panel, times(1)).showWarning(eq(MISSING_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  public void testCheckSdk_invalidSdk() throws InterruptedException {
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    when(cloudSdkService.isValidCloudSdk("/non/empty/path")).thenReturn(false);
    panel.checkSdk("/non/empty/path");
    verify(panel, times(1)).showWarning(eq(INVALID_SDK_DIR_WARNING));
    verify(panel, times(0)).hideWarning();
  }

  public void testCheckSdk_unsupportedSdk() {
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED);
    when(cloudSdkService.isValidCloudSdk("/non/empty/path")).thenReturn(false);
    panel.checkSdk("/non/empty/path");
    verify(panel, times(1))
        .showWarning(eq(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED.getMessage()));
    verify(panel, times(0)).hideWarning();
  }

  public void testCheckSdk_multipleValidationResults() {
    setValidateCloudSdkResponse(
        CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED,
        CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    when(cloudSdkService.isValidCloudSdk("/non/empty/path")).thenReturn(false);

    String expectedMessage =
        INVALID_SDK_DIR_WARNING
            + "<p>"
            + CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED.getMessage()
            + "</p>";

    panel.checkSdk("/non/empty/path");
    verify(panel, times(1)).showWarning(eq(expectedMessage));
    verify(panel, times(0)).hideWarning();
  }

  public void testCheckSdk_validSdk() {
    when(cloudSdkService.isValidCloudSdk("/non/empty/path")).thenReturn(true);
    setValidateCloudSdkResponse();
    panel.checkSdk("/non/empty/path");
    verify(panel, times(0)).showWarning(any(String.class));
    verify(panel, times(1)).hideWarning();
  }

  public void testApplyWith_invalidSdk() throws Exception {
    setValidateCloudSdkResponse(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    panel.getCloudSdkDirectoryField().setText("/non/empty/path");

    // No exception should be thrown on invalid sdk entry from this panel
    panel.apply();
  }

  private void setValidateCloudSdkResponse(CloudSdkValidationResult... results) {
    Set<CloudSdkValidationResult> validationResults = new HashSet<>();
    Collections.addAll(validationResults, results);
    when(cloudSdkService.validateCloudSdk(any(String.class))).thenReturn(validationResults);
  }
}
