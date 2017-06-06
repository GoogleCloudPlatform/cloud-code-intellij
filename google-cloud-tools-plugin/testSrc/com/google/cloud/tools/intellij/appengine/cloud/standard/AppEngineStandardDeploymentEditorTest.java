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

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.common.collect.ImmutableSet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import org.apache.commons.lang.StringUtils;
import org.mockito.Mock;
import org.picocontainer.MutablePicoContainer;

import java.util.HashSet;

public class AppEngineStandardDeploymentEditorTest extends PlatformTestCase {

  private static final String PROJECT_NAME = "test-proj";
  private AppEngineStandardDeploymentEditor editor;
  private AppEngineArtifactDeploymentSource deploymentSource;
  private ProjectSelector projectSelector;
  private CloudSdkService cloudSdkService;
  @Mock
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
    doNothing().when(infoPanel).refresh(anyString(), isA(Credential.class));

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineStandardDeploymentEditor(getProject(), deploymentSource);
    editor.setProjectSelector(projectSelector);
    editor.setApplicationInfoPanel(infoPanel);
  }

  public void testValidateConfiguration() throws ConfigurationException {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    editor.applyEditorTo(config);
  }

  public void testValidateConfiguration_invalidDeploymentSource() {
    when(deploymentSource.isValid()).thenReturn(false);
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();

    try {
      editor.applyEditorTo(config);
      fail("Invalid deployment source.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals("Select a valid deployment source."));
      assertNull(config.getVersion());
    }
  }

  public void testValidateConfiguration_blankProjectSelector() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    when(projectSelector.getText()).thenReturn("");

    try {
      editor.applyEditorTo(config);
      fail("No project selected.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals("Please select a project."));
      assertNull(config.getVersion());
    }
  }

  public void testValidateConfiguration_invalidApplication() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    when(infoPanel.isApplicationValid()).thenReturn(false);

    try {
      editor.applyEditorTo(config);
      fail("Invalid application.");
    } catch (ConfigurationException e) {
      assertTrue(e.getMessage().equals(
          "An App Engine application must be created before you can deploy to App Engine."));
      assertNull(config.getVersion());
    }
  }

  public void testValidateConfiguration_invalidCloudSdk() {
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

  public void testValidateConfiguration_missingJavaComponent() {
    when(cloudSdkService.validateCloudSdk()).thenReturn(
        ImmutableSet.of(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT));
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");

    try {
      editor.applyEditorTo(config);
      fail("Missing Java component validation error expected");
    } catch (ConfigurationException ce) {
      assertTrue(!StringUtils.isEmpty(ce.getMessage()));
      assertNull(config.getVersion());
    }
  }

  public void testPromoteAndStopDefaults() {
    assertFalse(editor.getCommonConfig().getPromoteCheckbox().isSelected());
    assertFalse(editor.getStopPreviousVersionCheckbox().isSelected());
    assertFalse(editor.getStopPreviousVersionCheckbox().isVisible());
  }

  public void testSelectPromote_doesntSelectStop() {
    editor.getCommonConfig().getPromoteCheckbox().setSelected(true);
    assertFalse(editor.getStopPreviousVersionCheckbox().isSelected());
    assertFalse(editor.getStopPreviousVersionCheckbox().isVisible());
  }

  public void testDeployAllConfigsDefaults() {
    assertTrue(editor.getDeployAllConfigsCheckbox().isVisible());
    assertFalse(editor.getDeployAllConfigsCheckbox().isSelected());
  }

  @Override
  protected void tearDown() throws Exception {
    Disposer.dispose(editor);
    super.tearDown();
  }
}
