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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.picocontainer.MutablePicoContainer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppEngineCloudConfigurableTest extends PlatformTestCase {
  private AppEngineCloudConfigurable appEngineCloudConfigurable;
  private CloudSdkService cloudSdkService;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final Path CLOUD_SDK_DIR_PATH = Paths.get("a", "b", "c", "gcloud-sdk");

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

  public void testSetupWithGoogleSettingSdkConfigured() throws Exception {
    when(cloudSdkService.getSdkHomePath()).thenReturn(CLOUD_SDK_DIR_PATH);
    initCloudConfigurable();
    appEngineCloudConfigurable.reset();

    assertEquals(CLOUD_SDK_DIR_PATH, Paths.get(cloudSdkDirectoryField.getText()));
  }

  public void testApply_invalidSdk() throws ConfigurationException {
    initCloudConfigurable();
    cloudSdkDirectoryField.setText("/some/invalid/path");

    try {
      appEngineCloudConfigurable.apply();
      fail("Applying settings without a valid SDK should throw exception.");
    } catch (ConfigurationException ce) {
      assertFalse(ce.getMessage().isEmpty());
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
}
