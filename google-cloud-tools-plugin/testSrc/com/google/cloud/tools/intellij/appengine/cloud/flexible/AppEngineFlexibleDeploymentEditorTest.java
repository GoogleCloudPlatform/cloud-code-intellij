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
import java.util.Optional;

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
    when(projectService.getFlexibleRuntimeFromAppYaml(isA(String.class)))
        .thenReturn(FlexibleRuntime.CUSTOM);
    when(projectService.getServiceNameFromAppYaml("custom")).thenReturn(Optional.of("customService"));
    when(projectService.getServiceNameFromAppYaml("java")).thenReturn(Optional.of("javaService"));
    when(projectService.getFlexibleRuntimeFromAppYaml("custom")).thenReturn(FlexibleRuntime.CUSTOM);
    when(projectService.getFlexibleRuntimeFromAppYaml("java")).thenReturn(FlexibleRuntime.JAVA);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineFlexibleDeploymentEditor(getProject(), deploymentSource);
    editor.getYamlTextField().setText("java");
    editor.getDockerfileTextField().setText("dockerfile 1");
  }

  public void testUpdateServiceName() {
    assertEquals(editor.getServiceLabel().getText(), "javaService");
    editor.getYamlTextField().setText("custom");
    assertEquals(editor.getServiceLabel().getText(), "customService");
  }

  public void testDockerfileSectionToggle() {
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    editor.getYamlTextField().setText("custom");
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
  }

  public void testPromote_StopPreviousVersion_Flexible() {
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
