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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.util.SystemEnvironmentProvider;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.picocontainer.MutablePicoContainer;

import java.io.File;

import javax.swing.JLabel;

/**
 * Tests for {@link CloudSdkPanel}.
 */
public class CloudSdkPanelTest extends PlatformTestCase {

  private CloudSdkPanel panel;

  private SystemEnvironmentProvider environmentProvider;
  private CloudSdkService cloudSdkService;

  private JLabel warningMessage;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String CLOUD_SDK_EXECUTABLE_PATH = new File("/a/b/c/gcloud-sdk/bin/gcloud").getAbsolutePath();
  private static final String CLOUD_SDK_DIR_PATH = new File("/a/b/c/gcloud-sdk").getAbsolutePath();

  private static final String MISSING_SDK_DIR_WARNING = "Please select a Cloud SDK home directory.";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    environmentProvider = mock(SystemEnvironmentProvider.class);
    cloudSdkService = mock(CloudSdkService.class);

    applicationContainer.unregisterComponent(SystemEnvironmentProvider.class.getName());
    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        SystemEnvironmentProvider.class.getName(), environmentProvider);
    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);
  }

  public void testSetupWithoutSdkInPath() {
    when(environmentProvider.findInPath(anyString())).thenReturn(null);
    initCloudSdkPanel();

    assertFalse(warningMessage.isVisible());
    assertEmpty(cloudSdkDirectoryField.getText());
  }

  public void testSetupWithSdkInPath() {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(new File(CLOUD_SDK_EXECUTABLE_PATH));
    initCloudSdkPanel();

    assertFalse(warningMessage.isVisible());
    assertEquals(CLOUD_SDK_DIR_PATH, cloudSdkDirectoryField.getText());
  }

  public void testSetupWithInvalidSdk() {
    initCloudSdkPanel();

    // Simulating user choosing (or manually typing) an invalid path in the field
    cloudSdkDirectoryField.setText("/some/invalid/path");

    assertTrue(warningMessage.isVisible());
    assertEquals(MISSING_SDK_DIR_WARNING, warningMessage.getText());
  }

  public void testApplyWith_invalidSdk() throws Exception {
    initCloudSdkPanel();
    cloudSdkDirectoryField.setText("/some/invalid/path");

    // No exception should be thrown on invalid sdk entry from this panel
    panel.apply();
  }

  private void initCloudSdkPanel() {
    panel = new CloudSdkPanel(cloudSdkService);

    warningMessage = panel.getWarningMessage();
    cloudSdkDirectoryField = panel.getCloudSdkDirectoryField();
  }
}
