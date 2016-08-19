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

import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.util.SystemEnvironmentProvider;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.junit.rules.TemporaryFolder;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

/**
 * Tests for {@link CloudSdkPanel}.
 */
public class CloudSdkPanelTest extends PlatformTestCase {

  private CloudSdkPanel panel;

  private SystemEnvironmentProvider environmentProvider;
  private AppEngineSdkService appEngineSdkService;

  private JLabel warningMessage;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String INVALID_SDK_DIR_WARNING = "No Cloud SDK was found in this directory.";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    environmentProvider = mock(SystemEnvironmentProvider.class);
    appEngineSdkService = mock(AppEngineSdkService.class);

    applicationContainer.unregisterComponent(SystemEnvironmentProvider.class.getName());
    applicationContainer.unregisterComponent(AppEngineSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        SystemEnvironmentProvider.class.getName(), environmentProvider);
    applicationContainer.registerComponentInstance(
        AppEngineSdkService.class.getName(), appEngineSdkService);
  }

  public void testSetupWithoutSdkInPath() {
    when(environmentProvider.findInPath(anyString())).thenReturn(null);
    initCloudSdkPanel();

    assertTrue(warningMessage.isVisible());
    assertEmpty(cloudSdkDirectoryField.getText());
  }

  public void testSetupWithSdkInPath() throws IOException {
    TemporaryFolder tempFolder = new TemporaryFolder();
    tempFolder.create();
    File executable = new File(tempFolder.newFolder("bin"), CloudSdkUtil.getSystemCommand());
    executable.createNewFile();

    when(environmentProvider.findInPath(anyString()))
        .thenReturn(executable);
    initCloudSdkPanel();

    assertFalse(warningMessage.isVisible());
    assertEquals(CloudSdkUtil.toSdkHomeDirectory(executable.getPath()),
        cloudSdkDirectoryField.getText());
  }

  public void testSetupWithInvalidSdk() {
    initCloudSdkPanel();

    // Simulating user choosing (or manually typing) an invalid path in the field
    cloudSdkDirectoryField.setText("/some/invalid/path");

    assertTrue(warningMessage.isVisible());
    assertEquals(INVALID_SDK_DIR_WARNING, warningMessage.getText());
  }

  public void testApplyWith_invalidSdk() throws Exception {
    initCloudSdkPanel();
    cloudSdkDirectoryField.setText("/some/invalid/path");

    // No exception should be thrown on invalid sdk entry from this panel
    panel.apply();
  }

  private void initCloudSdkPanel() {
    panel = new CloudSdkPanel(appEngineSdkService);

    warningMessage = panel.getWarningMessage();
    cloudSdkDirectoryField = panel.getCloudSdkDirectoryField();
  }
}
