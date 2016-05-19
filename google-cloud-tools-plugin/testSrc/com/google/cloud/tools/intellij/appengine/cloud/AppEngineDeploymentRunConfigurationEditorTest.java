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

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.elysium.ProjectSelector;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.PlatformTestCase;

public class AppEngineDeploymentRunConfigurationEditorTest extends PlatformTestCase {

  private AppEngineDeploymentRunConfigurationEditor editor;
  private DeploymentSource deploymentSource;
  private AppEngineHelper appEngineHelper;
  private ProjectSelector projectSelector;

  private static final String PROJECT_NAME = "test-proj";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    deploymentSource = mock(DeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);

    appEngineHelper = mock(AppEngineHelper.class);

    projectSelector = mock(ProjectSelector.class);
    when(projectSelector.getText()).thenReturn(PROJECT_NAME);

    editor = new AppEngineDeploymentRunConfigurationEditor(
        getProject(), deploymentSource, appEngineHelper);

    editor.setProjectSelector(projectSelector);
  }

  public void testValidSelections() {
    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);

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

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    Disposer.dispose(editor);
  }
}
