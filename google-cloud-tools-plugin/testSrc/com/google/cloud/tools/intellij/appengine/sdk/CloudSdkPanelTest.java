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

import static org.mockito.Mockito.mock;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.ui.JBColor;

import org.junit.After;
import org.picocontainer.MutablePicoContainer;

import javax.swing.JTextPane;

/**
 * Tests for {@link CloudSdkPanel}.
 */
public class CloudSdkPanelTest extends PlatformTestCase {

  private CloudSdkPanel panel;

  private CloudSdkService cloudSdkService;

  private JTextPane warningMessage;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String INVALID_SDK_DIR_WARNING = "<html>\n"
      + "  <head>\n"
      + "    \n"
      + "  </head>\n"
      + "  <body>\n"
      + "    No Cloud SDK was found in this directory. <a href=\"https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version\">Click \n"
      + "    here</a> to download the Cloud SDK.\n"
      + "  </body>\n"
      + "</html>\n";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    cloudSdkService = mock(CloudSdkService.class);

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();
    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(cloudSdkService);

  }

  public void testSetupWithInvalidSdk() {
    initCloudSdkPanel();

    // Simulating user choosing (or manually typing) an invalid path in the field
    cloudSdkDirectoryField.setText("/some/invalid/path");

    assertTrue(warningMessage.isVisible());
    assertEquals(cloudSdkDirectoryField.getTextField().getForeground(), JBColor.RED);
    assertEquals(INVALID_SDK_DIR_WARNING, warningMessage.getText());
  }

  public void testApplyWith_invalidSdk() throws Exception {
    initCloudSdkPanel();
    cloudSdkDirectoryField.setText("/some/invalid/path");

    // No exception should be thrown on invalid sdk entry from this panel
    panel.apply();
  }

  private void initCloudSdkPanel() {
    panel = new CloudSdkPanel();

    warningMessage = panel.getWarningMessage();
    cloudSdkDirectoryField = panel.getCloudSdkDirectoryField();
  }
}
