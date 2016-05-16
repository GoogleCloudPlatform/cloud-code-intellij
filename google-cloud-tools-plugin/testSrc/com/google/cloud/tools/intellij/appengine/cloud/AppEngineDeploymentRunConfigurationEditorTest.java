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
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.PlatformTestCase;

public class AppEngineDeploymentRunConfigurationEditorTest extends PlatformTestCase {

  private AppEngineDeploymentRunConfigurationEditor editor;
  private DeploymentSource deploymentSource;
  private AppEngineHelper appEngineHelper;
  private ProjectSelector projectSelector;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    deploymentSource = mock(DeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);

    appEngineHelper = mock(AppEngineHelper.class);

    projectSelector = mock(ProjectSelector.class);
    when(projectSelector.getText()).thenReturn("test-proj");
  }

  public void testValidSelections() throws ConfigurationException {
    editor = new AppEngineDeploymentRunConfigurationEditor(
        getProject(), deploymentSource, appEngineHelper);

    editor.setProjectSelector(projectSelector);

    AppEngineDeploymentConfiguration config = new AppEngineDeploymentConfiguration();
    config.setCloudProjectName("test-cloud-proj");
    config.setConfigType(ConfigType.AUTO);

    try {
      editor.applyEditorTo(config);
    } catch (ConfigurationException ce) {
      fail("No validation error expected");
    }
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    editor.dispose();
  }
}
