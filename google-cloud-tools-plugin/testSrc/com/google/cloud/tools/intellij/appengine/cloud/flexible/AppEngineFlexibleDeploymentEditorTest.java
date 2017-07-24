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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineApplicationInfoPanel;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.resources.ProjectSelector;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineFlexibleDeploymentEditor}. */
public final class AppEngineFlexibleDeploymentEditorTest {

  private static final String EMAIL = "example@example.com";

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private AppEngineArtifactDeploymentSource deploymentSource;
  @Mock private CredentialedUser credentialedUser;
  @Mock private ProjectSelector projectSelector;
  @Mock private AppEngineApplicationInfoPanel appInfoPanel;

  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module javaModule;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module customModule;

  @TestModule private Module nonFlexModule;

  @TestFile(name = "java.yaml", contents = "runtime: java\nservice: javaService")
  private File javaYaml;

  @TestFile(name = "custom.yaml", contents = "runtime: custom\nservice: customService")
  private File customYaml;

  @TestFile(
    name = "Dockerfile",
    contents = "FROM gcr.io/google_appengine/jetty\nADD target.war $JETTY_BASE/webapps/root.war"
  )
  private File dockerfile;

  @TestFile(name = "target")
  private File notAWar;

  private UserSpecifiedPathDeploymentSource userSpecifiedPathDeploymentSource;
  private AppEngineDeploymentConfiguration configuration;
  private AppEngineFlexibleDeploymentEditor editor;

  @Before
  public void setUp() throws Exception {
    FacetManager.getInstance(javaModule)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath(javaYaml.getPath());
    AppEngineFlexibleFacetConfiguration customConfig =
        FacetManager.getInstance(customModule)
            .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
            .getConfiguration();
    customConfig.setAppYamlPath(customYaml.getPath());
    customConfig.setDockerDirectory(dockerfile.getParentFile().getPath());

    userSpecifiedPathDeploymentSource =
        new UserSpecifiedPathDeploymentSource(
            ModulePointerManager.getInstance(testFixture.getProject())
                .create(UserSpecifiedPathDeploymentSource.moduleName));

    configuration = new AppEngineDeploymentConfiguration();
    editor = new AppEngineFlexibleDeploymentEditor(testFixture.getProject(), deploymentSource);
    editor.setAppInfoPanel(appInfoPanel);
    editor.getCommonConfig().setProjectSelector(projectSelector);
  }

  @After
  public void tearDown() throws Exception {
    editor.getAppYamlCombobox().removeAllItems();
    Disposer.dispose(editor);
  }

  @Test
  public void moduleSelector() {
    assertThat(editor.getAppYamlCombobox().getItemCount()).isEqualTo(2);
  }

  @Test
  public void applyEditorTo_withDefaultConfiguration_doesSetDefaults() throws Exception {
    editor.applyEditorTo(configuration);

    assertThat(configuration.getCloudProjectName()).isNull();
    assertThat(configuration.getGoogleUsername()).isNull();
    assertThat(configuration.getEnvironment()).isEqualTo(AppEngineEnvironment.APP_ENGINE_FLEX);
    assertThat(configuration.getUserSpecifiedArtifactPath()).isEmpty();
    assertThat(configuration.isPromote()).isFalse();
    assertThat(configuration.isStopPreviousVersion()).isFalse();
    assertThat(configuration.getVersion()).isEmpty();
    assertThat(configuration.isDeployAllConfigs()).isFalse();
    assertThat(configuration.getModuleName()).isEqualTo(javaModule.getName());
  }

  @Test
  public void applyEditorTo_doesSetVersion() throws Exception {
    String version = "some-version";
    editor.getCommonConfig().getVersionIdField().setText(version);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getVersion()).isEqualTo(version);
  }

  @Test
  public void applyEditorTo_doesSetPromote() throws Exception {
    editor.getCommonConfig().getPromoteCheckbox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.isPromote()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetCloudProjectName() throws Exception {
    String project = "some-project";
    when(projectSelector.getText()).thenReturn(project);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getCloudProjectName()).isEqualTo(project);
  }

  @Test
  public void applyEditorTo_doesSetStopPreviousVersion() throws Exception {
    editor.getCommonConfig().getStopPreviousVersionCheckbox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.isStopPreviousVersion()).isTrue();
  }

  @Test
  public void applyEditorTo_withUser_doesSetGoogleUsername() throws Exception {
    when(credentialedUser.getEmail()).thenReturn(EMAIL);
    when(projectSelector.getSelectedUser()).thenReturn(credentialedUser);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getGoogleUsername()).isEqualTo(EMAIL);
  }

  @Test
  public void applyEditorTo_notJarOrWarUserSpecified() throws Exception {
    userSpecifiedPathDeploymentSource.setFilePath(notAWar.getPath());
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(notAWar.getPath());

    editor.applyEditorTo(configuration);

    assertThat(configuration.getUserSpecifiedArtifactPath()).isEqualTo(notAWar.getPath());
  }

  @Test
  public void applyEditorTo_validWar() throws Exception {
    String war = "some-war.war";
    userSpecifiedPathDeploymentSource.setFilePath(war);
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(war);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getUserSpecifiedArtifactPath()).isEqualTo(war);
  }

  @Test
  public void applyEditorTo_validJar() throws Exception {
    String jar = "some-jar.jar";
    userSpecifiedPathDeploymentSource.setFilePath(jar);
    editor.setDeploymentSource(userSpecifiedPathDeploymentSource);
    editor.getArchiveSelector().setText(jar);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getUserSpecifiedArtifactPath()).isEqualTo(jar);
  }

  @Test
  public void flexibleConfig_javaAppYaml() {
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(javaModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertThat(editor.getDockerDirectoryPanel().isVisible()).isFalse();
    assertThat(editor.getRuntimePanel().isVisible()).isTrue();
    assertThat(editor.getRuntimePanel().getLabelText()).isEqualTo("java");
  }

  @Test
  public void flexibleConfig_customAppYaml() {
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(customModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertThat(editor.getDockerDirectoryPanel().isVisible()).isTrue();
    assertThat(editor.getRuntimePanel().isVisible()).isTrue();
    assertThat(editor.getRuntimePanel().getLabelText()).isEqualTo("custom");
  }

  @Test
  public void flexibleConfig_restoredFromPersistedConfiguration() {
    // set the stored app.yaml to the java yaml
    configuration.setModuleName(javaModule.getName());
    editor.resetEditorFrom(configuration);

    AppEngineFlexibleFacet javaModuleFacet = AppEngineFlexibleFacet.getFacetByModule(javaModule);
    assertThat(editor.getAppYamlCombobox().getSelectedItem()).isEqualTo(javaModuleFacet);

    // set the stored app.yaml to the custom yaml
    configuration.setModuleName(customModule.getName());
    editor.resetEditorFrom(configuration);

    AppEngineFlexibleFacet customModuleFacet =
        AppEngineFlexibleFacet.getFacetByModule(customModule);
    assertThat(editor.getAppYamlCombobox().getSelectedItem()).isEqualTo(customModuleFacet);
  }

  @Test
  public void flexibleConfig_unselectedWhenNullPersistedConfig() {
    configuration.setModuleName(null);
    editor.resetEditorFrom(configuration);
    assertThat(editor.getAppYamlCombobox().getSelectedItem()).isNull();
  }

  @Test
  public void appYamlEditButton_visibleWhenAppYamlSelected() {
    configuration.setModuleName(javaModule.getName());
    editor.resetEditorFrom(configuration);

    assertThat(editor.getEditAppYamlButton().isVisible()).isTrue();
  }

  @Test
  public void appYamlEditButton_hiddenWhenNoAppYamlSelected() {
    configuration.setModuleName(null);
    editor.resetEditorFrom(configuration);

    assertThat(editor.getEditAppYamlButton().isVisible()).isFalse();
  }

  @Test
  public void serviceNameIsUpdated_whenAppYamlSelectionChanges() {
    configuration.setModuleName(javaModule.getName());
    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getServiceLabel().getText()).isEqualTo("javaService");

    // Now update to custom service
    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModule(customModule);
    editor.getAppYamlCombobox().setSelectedItem(facet);

    assertThat(editor.getCommonConfig().getServiceLabel().getText()).isEqualTo("customService");
  }
}
