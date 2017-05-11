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
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.common.collect.ImmutableSet;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.PlatformTestCase;

import org.mockito.Mock;
import org.picocontainer.MutablePicoContainer;

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

  private UserSpecifiedPathDeploymentSource userSpecifiedPathDeploymentSource;
  private AppEngineFlexibleDeploymentEditor editor;
  private Module javaModule;
  private Module customModule;
  private File customYaml;
  private File javaYaml;
  private File dockerfile;
  private AppEngineDeploymentConfiguration templateConfig;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    customYaml = createTempFile("custom.yaml", "runtime: custom\nservice: flexService");
    javaYaml = createTempFile("java.yaml", "runtime: java");
    dockerfile = createTempFile("Dockerfile", "FROM gcr.io/google_appengine/jetty\n"
        + "ADD target.war $JETTY_BASE/webapps/root.war");

    javaModule = createModule("flex module");
    ApplicationManager.getApplication().runWriteAction(
        () -> {
          AppEngineFlexibleFacet flexJavaFacet = FacetManager.getInstance(javaModule).addFacet(
              AppEngineFlexibleFacet.getFacetType(), "flex facet", null /* underlyingFacet */);
          flexJavaFacet.getConfiguration().setAppYamlPath(javaYaml.getPath());
        });
    customModule = createModule("flex module 2");
    ApplicationManager.getApplication().runWriteAction(() -> {
      AppEngineFlexibleFacet flexCustomFacet = FacetManager.getInstance(customModule).addFacet(
          AppEngineFlexibleFacet.getFacetType(), "flex facet", null /* underlyingFacet */);
      flexCustomFacet.getConfiguration().setAppYamlPath(customYaml.getPath());
      flexCustomFacet.getConfiguration().setDockerDirectory(dockerfile.getPath());
    });
    createModule("non flex module");

    deploymentSource = mock(AppEngineArtifactDeploymentSource.class);
    when(deploymentSource.isValid()).thenReturn(true);
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    cloudSdkService = mock(CloudSdkService.class);
    when(cloudSdkService.validateCloudSdk()).thenReturn(new HashSet<>());

    appInfoPanel = mock(AppEngineApplicationInfoPanel.class);
    when(appInfoPanel.isApplicationValid()).thenReturn(true);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    applicationContainer.unregisterComponent(CloudSdkService.class.getName());

    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), cloudSdkService);

    editor = new AppEngineFlexibleDeploymentEditor(getProject(), deploymentSource);
    editor.setAppInfoPanel(appInfoPanel);
    editor.getGcpProjectSelector().setText("test project");

    userSpecifiedPathDeploymentSource = new UserSpecifiedPathDeploymentSource(
        ModulePointerManager.getInstance(getProject()).create(
            UserSpecifiedPathDeploymentSource.moduleName));

    templateConfig = new AppEngineDeploymentConfiguration();
    templateConfig.setCloudProjectName("test project");
  }

  public void testModuleSelector() {
    assertEquals(2, editor.getModulesWithFlexFacetComboBox().getItemCount());
  }

  public void testUpdateServiceName() {
    assertEquals("default", editor.getServiceLabel().getText());
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText(customYaml.getPath());
    assertEquals("flexService", editor.getServiceLabel().getText());
  }

  public void testDockerfileSectionToggle() {
    assertFalse(editor.getDockerfileLabel().isVisible());
    assertFalse(editor.getDockerfileTextField().isVisible());
    assertFalse(editor.getDockerfileOverrideCheckBox().isVisible());
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText(customYaml.getPath());
    assertTrue(editor.getDockerfileLabel().isVisible());
    assertTrue(editor.getDockerfileTextField().isVisible());
    assertTrue(editor.getDockerfileOverrideCheckBox().isVisible());
  }

  public void testValidateConfiguration() throws ConfigurationException {
    // javaModule
    editor.applyEditorTo(templateConfig);
    // customModule
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText(dockerfile.getPath());
    editor.applyEditorTo(templateConfig);
  }

  public void testValidateConfiguration_emptyUserSpecified() {
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText("");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Should fail with empty deployable");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullUserSpecified() {
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(null);
    try {
      editor.applyEditorTo(templateConfig);
      fail("Should fail with null deployable");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_notJarOrWarUserSpecified() throws IOException {
    File invalidWar = createTempFile("target", "");
    userSpecifiedPathDeploymentSource.setFilePath(invalidWar.getPath());
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(invalidWar.getPath());
    try {
      editor.applyEditorTo(templateConfig);
      fail("Not a war or jar artifact");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a JAR or WAR file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_validWar() throws IOException, ConfigurationException {
    File war = createTempFile("target.war", "");
    userSpecifiedPathDeploymentSource.setFilePath(war.getPath());
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(war.getPath());
    editor.applyEditorTo(templateConfig);
  }

  public void testValidateConfiguration_validJar() throws IOException, ConfigurationException {
    File jar = createTempFile("target.jar", "");
    userSpecifiedPathDeploymentSource.setFilePath(jar.getPath());
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(jar.getPath());
    editor.applyEditorTo(templateConfig);
  }

  public void testValidateConfiguration_invalidArtifact() {
    when(deploymentSource.isValid()).thenReturn(false);
    try {
      editor.applyEditorTo(templateConfig);
      fail("Should be invalid artifact.");
    } catch (ConfigurationException cfe) {
      assertEquals("Select a valid deployment source.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankProjectSelector() {
    editor.getGcpProjectSelector().setText("");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Project selector is blank.");
    } catch (ConfigurationException cfe) {
      assertEquals("Please select a project.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullProjectSelector() {
    editor.getGcpProjectSelector().setText(null);
    try {
      editor.applyEditorTo(templateConfig);
      fail("Project selector is null");
    } catch (ConfigurationException cfe) {
      assertEquals("Please select a project.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_invalidCloudSdk() {
    when(cloudSdkService.validateCloudSdk())
        .thenReturn(ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
    try {
      editor.applyEditorTo(templateConfig);
      fail("Project selector is null");
    } catch (ConfigurationException cfe) {
      assertEquals("The Cloud SDK is misconfigured. To fix, reconfigure the Server.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankYaml() {
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText("");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Blank yaml.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to an app.yaml file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullYaml() {
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText(null);
    try {
      editor.applyEditorTo(templateConfig);
      fail("Null yaml.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to an app.yaml file.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_unexistingYaml() {
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText("I don't exist");
    try {
      editor.applyEditorTo(templateConfig);
      fail("The yaml file doesn't exist.");
    } catch (ConfigurationException cfe) {
      assertEquals(
          "The specified app.yaml configuration file does not exist or is not a valid file."
              + " Set a valid file in Module Settings or use another one.",
          cfe.getMessage());
    }
  }

  public void testValidateConfiguration_directoryYaml() {
    editor.getAppYamlOverrideCheckBox().setSelected(true);
    editor.getAppYamlTextField().setText(javaYaml.getParentFile().getPath());
    try {
      editor.applyEditorTo(templateConfig);
      fail("The yaml file is a directory.");
    } catch (ConfigurationException cfe) {
      assertEquals(
          "The specified app.yaml configuration file does not exist or is not a valid file."
              + " Set a valid file in Module Settings or use another one.",
          cfe.getMessage());
    }
  }

  public void testValidateConfiguration_blankDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText("");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Blank dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Dockerfile.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText(null);
    try {
      editor.applyEditorTo(templateConfig);
      fail("Null dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals("Browse to a Dockerfile.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_unexistingDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText("I don't exist");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Unexisting dockerfile.");
    } catch (ConfigurationException cfe) {
      assertEquals(
          "The specified Dockerfile configuration file does not exist or is not a valid file."
          + " Set a valid file in Module Settings or use another one.",
          cfe.getMessage());
    }
  }

  public void testValidateConfiguration_directoryDockerfile() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    editor.getDockerfileOverrideCheckBox().setSelected(true);
    editor.getDockerfileTextField().setText(dockerfile.getParentFile().getPath());
    try {
      editor.applyEditorTo(templateConfig);
      fail("Dockerfile is a directory.");
    } catch (ConfigurationException cfe) {
      assertEquals(
          "The specified Dockerfile configuration file does not exist or is not a valid file."
          + " Set a valid file in Module Settings or use another one.",
          cfe.getMessage());
    }
  }

  public void testValidateConfiguration_invalidApplication() {
    when(appInfoPanel.isApplicationValid()).thenReturn(false);
    try {
      editor.applyEditorTo(templateConfig);
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
  }

  public void testDockerfileOverride() {
    editor.getModulesWithFlexFacetComboBox().setSelectedItem(customModule);
    String previousDockerfile = editor.getDockerfileTextField().getText();
    editor.getDockerfileOverrideCheckBox().doClick();
    // Tests that the first override will be the previous value
    assertEquals(previousDockerfile, editor.getDockerfileTextField().getText());

    editor.getDockerfileTextField().setText("an override");
    editor.getDockerfileOverrideCheckBox().doClick();
    // When override is unselected, value goes back to module setting
    assertEquals(previousDockerfile, editor.getDockerfileTextField().getText());

    editor.getDockerfileOverrideCheckBox().doClick();
    // Override is memorized and gets restored
    assertEquals("an override", editor.getDockerfileTextField().getText());
  }

  public void testDockerfileStartsDisabledAndWithModuleSetting() {
    ApplicationManager.getApplication().runWriteAction(
        () -> ModuleManager.getInstance(getProject()).disposeModule(javaModule));
    editor = new AppEngineFlexibleDeploymentEditor(getProject(), deploymentSource);
    assertEquals(1, editor.getModulesWithFlexFacetComboBox().getItemCount());
    assertFalse(editor.getDockerfileTextField().isEnabled());
    assertEquals(dockerfile.getPath(), editor.getDockerfileTextField().getText());
  }

  @Override
  public void tearDown() throws Exception {
    Disposer.dispose(editor);
    super.tearDown();
  }
}
