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

    customYaml = createTempFile("custom.yaml", "runtime: custom\nservice: customService");
    javaYaml = createTempFile("java.yaml", "runtime: java\nservice: javaService");
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
      flexCustomFacet.getConfiguration().setDockerDirectory(dockerfile.getParentFile().getPath());
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
    editor.getProjectSelector().setText("test project");

    userSpecifiedPathDeploymentSource = new UserSpecifiedPathDeploymentSource(
        ModulePointerManager.getInstance(getProject()).create(
            UserSpecifiedPathDeploymentSource.moduleName));

    templateConfig = new AppEngineDeploymentConfiguration();
    templateConfig.setCloudProjectName("test project");
  }

  public void testModuleSelector() {
    assertEquals(2, editor.getAppYamlCombobox().getItemCount());
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
    editor.getProjectSelector().setText("");
    try {
      editor.applyEditorTo(templateConfig);
      fail("Project selector is blank.");
    } catch (ConfigurationException cfe) {
      assertEquals("Please select a project.", cfe.getMessage());
    }
  }

  public void testValidateConfiguration_nullProjectSelector() {
    editor.getProjectSelector().setText(null);
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
    JCheckBox promoteCheckbox = editor.getCommonConfig().getPromoteCheckbox();
    JCheckBox stopPreviousVersionCheckbox = editor.getStopPreviousVersionCheckBox();

    assertFalse(promoteCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isEnabled());

    // Disable the promote checkbox and test that stopPreviousVersion behaves correctly
    promoteCheckbox.setSelected(false);

    assertFalse(stopPreviousVersionCheckbox.isSelected());
    assertFalse(stopPreviousVersionCheckbox.isEnabled());
  }

  public void testSelectPromote_enablesStop() {
    editor.getCommonConfig().getPromoteCheckbox().setSelected(true);
    assertTrue(editor.getStopPreviousVersionCheckBox().isSelected());
    assertTrue(editor.getStopPreviousVersionCheckBox().isVisible());
    assertTrue(editor.getStopPreviousVersionCheckBox().isEnabled());
  }

  public void testDeployAllConfigsDefaults() {
    assertTrue(editor.getCommonConfig().getDeployAllConfigsCheckbox().isVisible());
    assertFalse(editor.getCommonConfig().getDeployAllConfigsCheckbox().isSelected());
  }

  public void testFlexibleConfig_javaAppYaml() {
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(javaModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertFalse(editor.getDockerDirectoryPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("java", editor.getRuntimePanel().getLabelText());
  }

  public void testFlexibleConfig_customAppYaml() {
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(customModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertTrue(editor.getDockerDirectoryPanel().isVisible());
    assertTrue(editor.getRuntimePanel().isVisible());
    assertEquals("custom", editor.getRuntimePanel().getLabelText());
  }

  public void testFlexibleConfig_restoredFromPersistedConfiguration() {
    // set the stored app.yaml to the java yaml
    templateConfig.setModuleName(javaModule.getName());
    editor.resetEditorFrom(templateConfig);

    AppEngineFlexibleFacet javaModuleFacet =
        AppEngineFlexibleFacet.getFacetByModule(javaModule);
    assertEquals(javaModuleFacet, editor.getAppYamlCombobox().getSelectedItem());

    // set the stored app.yaml to the custom yaml
    templateConfig.setModuleName(customModule.getName());
    editor.resetEditorFrom(templateConfig);

    AppEngineFlexibleFacet customModuleFacet =
        AppEngineFlexibleFacet.getFacetByModule(customModule);
    assertEquals(customModuleFacet, editor.getAppYamlCombobox().getSelectedItem());
  }

  public void testFlexibleConfig_unselectedWhenNullPersistedConfig() {
    templateConfig.setModuleName(null);
    editor.resetEditorFrom(templateConfig);
    assertNull(editor.getAppYamlCombobox().getSelectedItem());
  }

  public void testAppYamlEditButton_visibleWhenAppYamlSelected() {
    templateConfig.setModuleName(javaModule.getName());
    editor.resetEditorFrom(templateConfig);

    assertTrue(editor.getEditAppYamlButton().isVisible());
  }

  public void testAppYamlEditButton_hiddenWhenNoAppYamlSelected() {
    templateConfig.setModuleName(null);
    editor.resetEditorFrom(templateConfig);

    assertFalse(editor.getEditAppYamlButton().isVisible());
  }

  public void testServiceNameIsUpdated_whenAppYamlSelectionChanges() {
    templateConfig.setModuleName(javaModule.getName());
    editor.resetEditorFrom(templateConfig);

    assertEquals("javaService", editor.getCommonConfig().getServiceLabel().getText());

    // Now update to custom service
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(customModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertEquals("customService", editor.getCommonConfig().getServiceLabel().getText());
  }

  @Override
  public void tearDown() throws Exception {
    editor.getAppYamlCombobox().removeAllItems();
    Disposer.dispose(editor);
    super.tearDown();
  }
}
