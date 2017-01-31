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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.common.collect.ImmutableSet;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import org.mockito.Mock;
import org.picocontainer.MutablePicoContainer;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.JCheckBox;

public class AppEngineFlexibleDeploymentEditorTest extends PlatformTestCase {

  @Mock
  private CloudSdkService cloudSdkService;
  @Mock
  private AppEngineApplicationInfoPanel appInfoPanel;
  @Mock
  private AppEngineArtifactDeploymentSource deploymentSource;
  @Mock
  private UserSpecifiedPathDeploymentSource userSpecifiedPathDeploymentSource;

  private AppEngineFlexibleDeploymentEditor editor;
  private Module javaModule;
  private Module customModule;
  private File customYaml;
  private File javaYaml;
  private File dockerfile;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    customYaml = createTempFile("custom.yaml", "runtime: custom\nservice: flexService");
    javaYaml = createTempFile("java.yaml", "runtime: java");
    dockerfile = createTempFile("Dockerfile", "FROM gcr.io/google_appengine/jetty\n"
        + "ADD target.war $JETTY_BASE/webapps/root.war");

    javaModule = createModule("flex module");
    AppEngineFlexibleFacet flexJavaFacet = FacetManager.getInstance(javaModule).addFacet(
        AppEngineFlexibleFacet.getFacetType(), "flex facet", null /* underlyingFacet */);
    flexJavaFacet.getConfiguration().setAppYamlPath(javaYaml.getPath());
    customModule = createModule("flex module 2");
    AppEngineFlexibleFacet flexCustomFacet = FacetManager.getInstance(customModule).addFacet(
        AppEngineFlexibleFacet.getFacetType(), "flex facet", null /* underlyingFacet */);
    flexCustomFacet.getConfiguration().setAppYamlPath(customYaml.getPath());
    flexCustomFacet.getConfiguration().setDockerfilePath(dockerfile.getPath());
    createModule("non flex module");

    deploymentSource = mock(AppEngineArtifactDeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);
    userSpecifiedPathDeploymentSource = mock(UserSpecifiedPathDeploymentSource.class);

    cloudSdkService = mock(CloudSdkService.class);
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    appInfoPanel = mock(AppEngineApplicationInfoPanel.class);
    when(appInfoPanel.isValid()).thenReturn(true);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineFlexibleDeploymentEditor(getProject(), deploymentSource);
    editor.getYamlTextField().setText("java");
    editor.getDockerfileTextField().setText("dockerfile 1");
    editor.setAppInfoPanel(appInfoPanel);
  }

  public void testModuleSelector() {
    assertEquals(2, editor.getModulesWithFlexFacetComboBox().getItemCount());
  }

  public void testUpdateServiceName() {
    assertEquals("default", editor.getServiceLabel().getText());
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText(customYaml.getPath());
    assertEquals("flexService", editor.getServiceLabel().getText());
  }

  public void testDockerfileSectionToggle() {
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText(customYaml.getPath());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
  }

  public void testCheckConfigurationFiles() {
    // runtime: java
    assertEquals(Color.BLACK, editor.getYamlTextField().getForeground());
    assertEquals(Color.BLACK, editor.getYamlLabel().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    // non existing file
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText("I don't exist");
    assertEquals(Color.RED, editor.getYamlTextField().getForeground());
    assertEquals(Color.RED, editor.getYamlLabel().getForeground());
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    assertTrue(editor.getFilesWarningLabel().isVisible());
    // runtime: custom, no good dockerfile
    editor.getYamlTextField().setText(customYaml.getPath());
    assertEquals(Color.BLACK, editor.getYamlTextField().getForeground());
    assertEquals(Color.BLACK, editor.getYamlLabel().getForeground());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
    assertEquals(Color.RED, editor.getDockerfileLabel().getForeground());
    assertTrue(editor.getFilesWarningLabel().isVisible());
    // runtime: custom, good dockerfile
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText(dockerfile.getPath());
    assertEquals(Color.BLACK, editor.getYamlTextField().getForeground());
    assertEquals(Color.BLACK, editor.getYamlLabel().getForeground());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
    assertEquals(Color.BLACK, editor.getDockerfileLabel().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    // second module from combobox
    editor.getYamlOverrideCheckBox().setSelected(false);
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    assertEquals(Color.BLACK, editor.getYamlTextField().getForeground());
    assertEquals(Color.BLACK, editor.getYamlLabel().getForeground());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
    assertEquals(Color.BLACK, editor.getDockerfileLabel().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    // back to first module
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(javaModule);
    assertEquals(Color.BLACK, editor.getYamlTextField().getForeground());
    assertEquals(Color.BLACK, editor.getYamlLabel().getForeground());
    assertFalse(editor.getFilesWarningLabel().isVisible());
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    assertFalse(editor.getFilesWarningLabel().isVisible());
  }

  public void testValidateConfiguration() throws ConfigurationException {
    // javaModule
    editor.applyEditorTo(editor.getSnapshot());
    // customModule
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileTextField().setText(dockerfile.getPath());
    editor.applyEditorTo(editor.getSnapshot());
  }

  public void testValidateConfiguration_emptyUserSpecified() {
    editor = new AppEngineFlexibleDeploymentEditor(getProject(), userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText("");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Should fail with empty deployable");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullUserSpecified() {
    editor = new AppEngineFlexibleDeploymentEditor(getProject(), userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(null);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Should fail with null deployable");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_notJarOrWarUserSpecified() throws IOException {
    userSpecifiedPathDeploymentSource.setFilePath(createTempFile("target", "").getPath());
    editor = new AppEngineFlexibleDeploymentEditor(getProject(), userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(userSpecifiedPathDeploymentSource.getFile().getPath());
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Not a war or jar artifact");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_invalidArtifact() {
    when(deploymentSource.isValid()).thenReturn(false);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Should be invalid artifact.");
    } catch (ConfigurationException cfe) {
      assertEquals("Select a valid deployment source.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankProjectSelector() {
    editor.getGcpProjectSelector().setText("");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Project selector is blank.");
    } catch (ConfigurationException cfe) {
      assertEquals("Please select a project.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullProjectSelector() {
    editor.getGcpProjectSelector().setText(null);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Project selector is null");
    } catch (ConfigurationException cfe) {
      assertEquals("Please select a project.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_invalidCloudSdk() {
    when(cloudSdkService.validateCloudSdk())
        .thenReturn(ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Project selector is null");
    } catch (ConfigurationException cfe) {
      assertEquals("The Cloud SDK is misconfigured. To fix, reconfigure the Server.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankYaml() {
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText("");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Blank yaml.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Yaml file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullYaml() {
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText(null);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Null yaml.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Yaml file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_unexistingYaml() {
    editor.getYamlOverrideCheckBox().setSelected(true);
    editor.getYamlTextField().setText("I don't exist");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("The yaml file doesn't exist.");
    } catch (ConfigurationException cfe) {
      assertEquals("The specified Yaml configuration file does not exist.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileTextField().setText("");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Blank dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Dockerfile.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileTextField().setText(null);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Null dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Dockerfile.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_unexistingDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileTextField().setText("I don't exist");
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Unexisting dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals("The specified Dockerfile configuration file does not exist.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_invalidApplication() {
    when(appInfoPanel.isValid()).thenReturn(false);
    try {
      editor.applyEditorTo(editor.getSnapshot());
      fail("Invalid application.");
    } catch (ConfigurationException cfe) {
      assertEquals("An App Engine application must be created before you can deploy to App Engine.",
          cfe.getMessage());
    }
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
