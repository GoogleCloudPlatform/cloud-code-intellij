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

import com.google.gct.idea.elysium.ProjectSelector;
import com.google.gct.idea.util.SystemEnvironmentProvider;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.junit.rules.TemporaryFolder;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;

public class ManagedVmCloudConfigurableTest extends PlatformTestCase {
  private ManagedVmCloudConfigurable managedVmCloudConfigurable;
  private SystemEnvironmentProvider environmentProvider;
  private ProjectSelector projectSelector;
  private JLabel warningMessage;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String CLOUD_SDK_EXECUTABLE_PATH = "/a/b/c/gcloud";
  private static final String CLOUD_SDK_DIR_PATH = "/a/b/c";

  private static final String MISSING_PROJECT_WARNING = "Please select a project.";
  private static final String MISSING_SDK_DIR_WARNING = "Please select a Cloud SDK directory.";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    environmentProvider = mock(SystemEnvironmentProvider.class);

    applicationContainer.unregisterComponent(SystemEnvironmentProvider.class.getName());
    applicationContainer.registerComponentInstance(
        SystemEnvironmentProvider.class.getName(), environmentProvider);
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
    assertEquals(MISSING_SDK_DIR_WARNING, warningMessage.getText());
  }

  public void testApply_validSdkAndValidProject() throws Exception {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(createTempFile());
    initCloudConfigurable();
    projectSelector.setText("myProject");

    // No exception should be thrown here
    managedVmCloudConfigurable.apply();
  }

  public void testApply_validSdkAndInvalidProject() throws IOException {
    when(environmentProvider.findInPath(anyString())).thenReturn(createTempFile());
    initCloudConfigurable();
    // Do not select project

    try {
      managedVmCloudConfigurable.apply();
      fail("Applying settings without a Project should throw exception.");
    } catch (ConfigurationException ce) {
      assertEquals(MISSING_PROJECT_WARNING, ce.getMessage());
    }
  }

  public void testApply_invalidSdkAndValidProject() {
    initCloudConfigurable();
    cloudSdkDirectoryField.setText("/some/invalid/path");
    projectSelector.setText("myProject");

    try {
      managedVmCloudConfigurable.apply();
      fail("Applying settings without a valid SDK should throw exception.");
    } catch (ConfigurationException ce) {
      assertEquals(MISSING_SDK_DIR_WARNING, ce.getMessage());
    }
  }

  public void testApply_invalidSdkAndInvalidProject() {
    initCloudConfigurable();
    cloudSdkDirectoryField.setText("/some/invalid/path");
    // Do not select project

    try {
      managedVmCloudConfigurable.apply();
      fail("Applying settings without a valid SDK and Project should throw exception.");
    } catch (ConfigurationException ce) {
      assertEquals(MISSING_SDK_DIR_WARNING, ce.getMessage());
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void initCloudConfigurable() {
    managedVmCloudConfigurable =
        new ManagedVmCloudConfigurable(new ManagedVmServerConfiguration(), getProject());
    projectSelector = managedVmCloudConfigurable.getProjectSelector();
    warningMessage = managedVmCloudConfigurable.getWarningMessage();
    cloudSdkDirectoryField = managedVmCloudConfigurable.getCloudSdkDirectoryField();
  }

  private File createTempFile() throws IOException {
    TemporaryFolder tempFolder = new TemporaryFolder();
    tempFolder.create();
    return tempFolder.newFile("gcloud");
  }
}
