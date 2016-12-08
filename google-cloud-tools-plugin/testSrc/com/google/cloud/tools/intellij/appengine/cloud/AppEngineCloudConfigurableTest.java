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

import com.google.api.client.repackaged.javax.annotation.concurrent.Immutable;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.common.collect.ImmutableSet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.PlatformTestCase;

import org.picocontainer.MutablePicoContainer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AppEngineCloudConfigurableTest extends PlatformTestCase {
  private AppEngineCloudConfigurable appEngineCloudConfigurable;
  private CloudSdkService cloudSdkService;
  private TextFieldWithBrowseButton cloudSdkDirectoryField;

  private static final Path CLOUD_SDK_DIR_PATH = Paths.get("a", "b", "c", "gcloud-sdk");
  private static final String INVALID_SDK_DIR_WARNING =
      "No Cloud SDK was found in the specified directory.";

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
    when(cloudSdkService.validateCloudSdk(anyString()))
        .thenReturn(ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
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
}
