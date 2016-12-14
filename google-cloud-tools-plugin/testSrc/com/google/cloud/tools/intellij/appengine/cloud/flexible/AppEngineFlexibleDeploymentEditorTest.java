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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import org.picocontainer.MutablePicoContainer;

import java.util.HashSet;

import javax.swing.JCheckBox;

public class AppEngineFlexibleDeploymentEditorTest extends PlatformTestCase {

  private CloudSdkService cloudSdkService;
  private AppEngineProjectService projectService;
  private AppEngineArtifactDeploymentSource deploymentSource;
  private AppEngineFlexibleDeploymentEditor editor;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    deploymentSource = mock(AppEngineArtifactDeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);

    cloudSdkService = mock(CloudSdkService.class);
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    projectService = mock(AppEngineProjectService.class);
    when(projectService.getFlexibleRuntimeFromAppYaml(isA(String.class))).thenReturn(
        FlexibleRuntime.CUSTOM);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineFlexibleDeploymentEditor(getProject(), deploymentSource);
    editor.getYamlTextField().setText("app yaml 1");
    editor.getDockerfileTextField().setText("dockerfile 1");
  }

  public void testUpdateFileOverrides() {
    editor.getOverrideFileLocationsCheckBox().setSelected(true);
    editor.getYamlTextField().setText("new app.yaml location");
    editor.getDockerfileTextField().setText("new dockerfile location");
    // Check that the override is properly memorized.
    assertEquals("new app.yaml location", editor.getYamlPathOverride());
    assertEquals("new dockerfile location", editor.getDockerfilePathOverride());

    editor.getOverrideFileLocationsCheckBox().setSelected(false);
    // Check that the field is switched back when override is disabled.
    assertEquals("app yaml 1", editor.getYamlTextField().getText());
    assertEquals("dockerfile 1", editor.getDockerfileTextField().getText());

    // Check that override works.
    editor.getOverrideFileLocationsCheckBox().setSelected(true);
    assertEquals("new app.yaml location", editor.getYamlPathOverride());
    assertEquals("new dockerfile location", editor.getDockerfilePathOverride());
  }

  public void testPromote_StopPreviousVersion_Flexible() {
//    when(deploymentSource.getEnvironment())
//        .thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    JCheckBox promoteCheckbox = editor.getPromoteVersionCheckBox();
    JCheckBox stopPreviousVersionCheckbox = editor.getStopPreviousVersionCheckBox();

    assertFalse(promoteCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isEnabled());

    // Disable the promote checkbox and test that stopPreviousVersion behaves correctly
    promoteCheckbox.setSelected(false);

    assertFalse(stopPreviousVersionCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isEnabled());

    Disposer.dispose(editor);
  }

  @Override
  public void tearDown() throws Exception {
    Disposer.dispose(editor);
    super.tearDown();
  }
}
