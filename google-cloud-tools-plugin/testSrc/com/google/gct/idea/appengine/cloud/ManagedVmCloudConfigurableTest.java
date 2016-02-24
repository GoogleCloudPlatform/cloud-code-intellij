/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gct.idea.util.SystemEnvironmentProvider;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.picocontainer.MutablePicoContainer;

import java.io.File;

import javax.swing.JLabel;

public class ManagedVmCloudConfigurableTest extends PlatformTestCase {
  private SystemEnvironmentProvider environmentProvider;
  private JLabel warningMessage;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String CLOUD_SDK_EXECUTABLE_PATH = "/a/b/c/gcloud";
  private static final String CLOUD_SDK_DIR_PATH = "/a/b/c";

  private static final String INVALID_SDK_DIR_WARNING = "Please select a Cloud SDK directory.";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    environmentProvider = mock(SystemEnvironmentProvider.class);

    applicationContainer.unregisterComponent(SystemEnvironmentProvider.class.getName());
    applicationContainer.registerComponentInstance(SystemEnvironmentProvider.class.getName(), environmentProvider);
  }

  public void testSetupWithoutSdkInPath() {
    when(environmentProvider.findInPath(anyString())).thenReturn(null);
    initCloudConfigurable();

    assertFalse(warningMessage.isVisible());
    assertEmpty(cloudSdkDirectoryField.getText());
  }

  public void testSetupWithSdkInPath() {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(new File(CLOUD_SDK_EXECUTABLE_PATH));
    initCloudConfigurable();

    assertFalse(warningMessage.isVisible());
    assertEquals(CLOUD_SDK_DIR_PATH, cloudSdkDirectoryField.getText());
  }

  public void testSetupWithInvalidSdk() {
    initCloudConfigurable();

    // Simulating user choosing (or manually typing) an invalid path in the field
    cloudSdkDirectoryField.setText("/some/invalid/path");

    assertTrue(warningMessage.isVisible());
    assertEquals(INVALID_SDK_DIR_WARNING, warningMessage.getText());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void initCloudConfigurable() {
    ManagedVmCloudConfigurable managedVmCloudConfigurable =
        new ManagedVmCloudConfigurable(new ManagedVmServerConfiguration(), getProject());
    warningMessage = managedVmCloudConfigurable.getWarningMessage();
    cloudSdkDirectoryField = managedVmCloudConfigurable.getCloudSdkDirectoryField();
  }
}
