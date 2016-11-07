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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.resources.ProjectSelector;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.containers.HashSet;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.MutablePicoContainer;

import java.nio.file.Path;
import java.util.Set;

import javax.swing.JCheckBox;

public class AppEngineDeploymentRunConfigurationEditorTest extends PlatformTestCase {

  private static final String PROJECT_NAME = "test-proj";
  private AppEngineDeploymentRunConfigurationEditor editor;
  private AppEngineArtifactDeploymentSource deploymentSource;
  private AppEngineHelper appEngineHelper;
  private ProjectSelector projectSelector;
  private CloudSdkService cloudSdkService;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    deploymentSource = mock(AppEngineArtifactDeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);

    appEngineHelper = mock(AppEngineHelper.class);

    projectSelector = mock(ProjectSelector.class);
    when(projectSelector.getText()).thenReturn(PROJECT_NAME);

    cloudSdkService = mock(CloudSdkService.class);
    when(cloudSdkService.validateCloudSdk(any(Path.class)))
        .thenReturn(new HashSet<CloudSdkValidationResult>());

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = createEditor(AppEngineEnvironment.APP_ENGINE_STANDARD);
  }

  public void testValidSelections() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);
    when(cloudSdkService.hasJavaComponent()).thenReturn(true);

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException ce) {
      fail("No validation error expected");
    }
  }

  public void testOnValidationFailure_configIsNotUpdated() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();

    // Simulate updating the config type in the UI then saving with an invalid configuration.
    // The resultant configuration should not contain the update.
    editor.getConfigTypeComboBox().setSelectedItem(ConfigType.CUSTOM);

    try {
      editor.applyEditorTo(config);
      fail("Expected validation failure");
    } catch (ConfigurationException ce) {
      assertEquals(ConfigType.AUTO, config.getConfigType());
    }
  }

  public void testValidationFailureStandardEnv_missingJavaComponent() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);

    try {
      editor.applyEditorTo(config);
      fail("Missing Java component validation error expected");
    } catch (ConfigurationException ce) {
      assertTrue(!StringUtils.isEmpty(ce.getMessage()));
    }
  }

  public void testValidationFailure_cloudSdkWarning() {
    CloudSdkValidationResult result = CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED;
    mockCloudSdkValidationResult(result);

    try {
      editor.applyEditorTo(mock(AppEngineDeploymentConfiguration.class));
      fail("Configuration warning expected");
    } catch (RuntimeConfigurationWarning rcw) {
      assertEquals(result.getMessage(), rcw.getMessage());
    } catch (ConfigurationException e) {
      fail("Thrown wrong Configuration Exception type. Expected RuntimeConfigurationWarning");
    }
  }

  public void testValidationFailure_cloudSdkError() {
    CloudSdkValidationResult result = CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND;
    mockCloudSdkValidationResult(result);

    try {
      editor.applyEditorTo(mock(AppEngineDeploymentConfiguration.class));
      fail("Configuration error expected");
    } catch (RuntimeConfigurationError rce) {
      assertEquals(result.getMessage(), rce.getMessage());
    } catch (ConfigurationException e) {
      fail("Thrown wrong Configuration Exception type. Expected RuntimeConfigurationError");
    }

  }

  private void mockCloudSdkValidationResult(CloudSdkValidationResult... results) {
    Set<CloudSdkValidationResult> resultSet =new HashSet<>();
    for (CloudSdkValidationResult result : results) {
      resultSet.add(result);
    }
    when(cloudSdkService.validateCloudSdk(any(Path.class)))
        .thenReturn(resultSet);
  }

  public void testValidationSuccessFlexEnv_missingJavaComponent() {
    AppEngineDeploymentRunConfigurationEditor editor
        = createEditor(AppEngineEnvironment.APP_ENGINE_FLEX);
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException ce) {
      fail("Expected validation failure");
    }
  }

  public void testUiAppEngineStandardEnvironment() {
    when(deploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);
    AppEngineDeploymentRunConfigurationEditor editor =
        new AppEngineDeploymentRunConfigurationEditor(
            getProject(),
            deploymentSource,
            appEngineHelper);

    assertEquals("App Engine Standard Environment", editor.getEnvironmentLabel().getText());
    assertFalse(editor.getAppEngineFlexConfigPanel().isVisible());
    Disposer.dispose(editor);
  }

  public void testUiAppEngineFlexEnvironment() {
    when(deploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);
    AppEngineDeploymentRunConfigurationEditor editor =
        new AppEngineDeploymentRunConfigurationEditor(
            getProject(), deploymentSource, appEngineHelper);

    assertEquals("App Engine Flexible Environment", editor.getEnvironmentLabel().getText());
    assertTrue(editor.getAppEngineFlexConfigPanel().isVisible());
    Disposer.dispose(editor);
  }

  public void testPromote_StopPreviousVersion_Standard() {
    when(deploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);
    AppEngineDeploymentRunConfigurationEditor editor =
        new AppEngineDeploymentRunConfigurationEditor(
            getProject(), deploymentSource, appEngineHelper);

    JCheckBox promoteCheckbox = editor.getPromoteCheckbox();
    JCheckBox stopPreviousVersionCheckbox = editor.getStopPreviousVersionCheckbox();

    assertTrue(promoteCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isVisible());

    Disposer.dispose(editor);
  }

  public void testPromote_StopPreviousVersion_Flexible() {
    when(deploymentSource.getEnvironment())
        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);
    AppEngineDeploymentRunConfigurationEditor editor =
        new AppEngineDeploymentRunConfigurationEditor(
            getProject(), deploymentSource, appEngineHelper);

    JCheckBox promoteCheckbox = editor.getPromoteCheckbox();
    JCheckBox stopPreviousVersionCheckbox = editor.getStopPreviousVersionCheckbox();

    assertTrue(promoteCheckbox.isSelected());
    assertTrue(stopPreviousVersionCheckbox.isSelected());
    assertTrue(stopPreviousVersionCheckbox.isVisible());
    assertTrue(stopPreviousVersionCheckbox.isEnabled());

    // Disable the promote checkbox and test that stopPreviousVersion behaves correctly
    promoteCheckbox.setSelected(false);

    assertFalse(stopPreviousVersionCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isEnabled());

    Disposer.dispose(editor);
  }

  private AppEngineDeploymentRunConfigurationEditor createEditor(AppEngineEnvironment environment) {
    when(deploymentSource.getEnvironment()).thenReturn(environment);

    AppEngineDeploymentRunConfigurationEditor editor
        = new AppEngineDeploymentRunConfigurationEditor(getProject(),
        deploymentSource, appEngineHelper);

    editor.setProjectSelector(projectSelector);

    return editor;
  }

  @Override
  protected void tearDown() throws Exception {
    Disposer.dispose(editor);
    super.tearDown();
  }
}
