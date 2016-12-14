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

package com.google.cloud.tools.intellij.appengine.cloud.standard;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.common.collect.ImmutableSet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.MutablePicoContainer;

import java.util.HashSet;

public class AppEngineStandardDeploymentEditorTest extends PlatformTestCase {

  private static final String PROJECT_NAME = "test-proj";
  private AppEngineStandardDeploymentEditor editor;
  private AppEngineArtifactDeploymentSource deploymentSource;
  private ProjectSelector projectSelector;
  private CloudSdkService cloudSdkService;
  private AppEngineApplicationInfoPanel infoPanel;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    deploymentSource = mock(AppEngineArtifactDeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);

    projectSelector = mock(ProjectSelector.class);
    when(projectSelector.getText()).thenReturn(PROJECT_NAME);

    cloudSdkService = mock(CloudSdkService.class);
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    infoPanel = mock(AppEngineApplicationInfoPanel.class);
    when(infoPanel.isApplicationValid()).thenReturn(true);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineStandardDeploymentEditor(getProject(), deploymentSource);
    editor.setProjectSelector(projectSelector);
    editor.setApplicationInfoPanel(infoPanel);
  }

  public void testValidSelections() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException ce) {
      fail("No validation error expected");
    }
  }

  public void testInvalidDeploymentSource() {
    when(deploymentSource.isValid()).thenReturn(false);
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals("Select a valid deployment source."));
      assertNull(config.getVersion());
    }
  }

  public void testBlankProjectSelector() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    when(projectSelector.getText()).thenReturn("");

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals("Please select a project."));
      assertNull(config.getVersion());
    }
  }

  public void testInvalidApplication() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    when(infoPanel.isApplicationValid()).thenReturn(false);

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals(
          "An App Engine application must be created before you can deploy to App Engine."));
      assertNull(config.getVersion());
    }
  }

  public void testInvalidCloudSdk() {
    when(cloudSdkService.validateCloudSdk()).thenReturn(
        ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");

    try {
      editor.applyEditorTo(config);
      fail("Cloud SDK not found validation error expected");
    } catch (ConfigurationException ce) {
      assertTrue(!StringUtils.isEmpty(ce.getMessage()));
      assertNull(config.getVersion());
    }
  }

  public void testMissingJavaComponent() {
    when(cloudSdkService.validateCloudSdk()).thenReturn(
        ImmutableSet.of(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT));
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);

    try {
      editor.applyEditorTo(config);
      fail("Missing Java component validation error expected");
    } catch (ConfigurationException ce) {
      assertTrue(!StringUtils.isEmpty(ce.getMessage()));
      assertNull(config.getVersion());
    }
  }

  public void testUnselectPromote() {
    assertTrue(editor.getPromoteCheckbox().isSelected());
    editor.getPromoteCheckbox().setSelected(false);
    assertFalse(editor.getStopPreviousVersionCheckbox().isEnabled());
    assertFalse(editor.getStopPreviousVersionCheckbox().isSelected());

    Disposer.dispose(editor);
  }

  @Override
  protected void tearDown() throws Exception {
    Disposer.dispose(editor);
    super.tearDown();
  }
}
