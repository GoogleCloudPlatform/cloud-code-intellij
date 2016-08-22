/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.util.SystemEnvironmentProvider;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.junit.rules.TemporaryFolder;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;

public class AppEngineCloudConfigurableTest extends PlatformTestCase {
  private AppEngineCloudConfigurable appEngineCloudConfigurable;
  private SystemEnvironmentProvider environmentProvider;
  private CloudSdkService cloudSdkService;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final String CLOUD_SDK_DIR_PATH = new File("/a/b/c/gcloud-sdk").getAbsolutePath();
  private static final String INVALID_SDK_DIR_WARNING = "No Cloud SDK was found in this directory.";

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

  public void testSetupWithGoogleSettingSdkConfigured() throws Exception {
    when(cloudSdkService.getSdkHomePath()).thenReturn(CLOUD_SDK_DIR_PATH);
    initCloudConfigurable();
    appEngineCloudConfigurable.reset();

    assertEquals(CLOUD_SDK_DIR_PATH, cloudSdkDirectoryField.getText());
  }

  public void testApply_validSdk() throws Exception {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(createTempFile());
    initCloudConfigurable();

    // No exception should be thrown here
    appEngineCloudConfigurable.apply();
  }

  public void testApply_invalidSdk() {
    initCloudConfigurable();
    cloudSdkDirectoryField.setText("/some/invalid/path");

    try {
      appEngineCloudConfigurable.apply();
      fail("Applying settings without a valid SDK should throw exception.");
    } catch (ConfigurationException ce) {
      assertEquals(INVALID_SDK_DIR_WARNING, ce.getMessage());
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void initCloudConfigurable() {
    appEngineCloudConfigurable =
        new AppEngineCloudConfigurable();

    CloudSdkPanel panel = appEngineCloudConfigurable.getCloudSdkPanel();

    cloudSdkDirectoryField = panel.getCloudSdkDirectoryField();
  }

  private File createTempFile() throws IOException {
    TemporaryFolder tempFolder = new TemporaryFolder();
    tempFolder.create();
    File executable = new File(tempFolder.newFolder("bin"), CloudSdkUtil.getSystemCommand());
    executable.createNewFile();
    return executable;
  }
}
