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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.project.ProjectSelector;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import javax.swing.JTabbedPane;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineStandardDeploymentEditor}. */
@RunWith(JUnit4.class)
public final class AppEngineStandardDeploymentEditorTest {

  private static final String EMAIL = "example@example.com";

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private AppEngineArtifactDeploymentSource deploymentSource;
  @Mock private CredentialedUser credentialedUser;
  @Mock private ProjectSelector projectSelector;
  @Mock @TestService private AppEngineProjectService mockAppEngineProjectService;

  @TestFixture private IdeaProjectTestFixture testFixture;

  private AppEngineDeploymentConfiguration configuration;
  private AppEngineStandardDeploymentEditor editor;

  @Before
  public void setUp() {
    configuration = new AppEngineDeploymentConfiguration();
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);
    editor.getCommonConfig().setProjectSelector(projectSelector);
  }

  @After
  public void tearDown() throws Exception {
    Disposer.dispose(editor);
  }

  @Test
  public void newInstance_doesSetServiceLabel() {
    String serviceName = "some-service-name";
    when(mockAppEngineProjectService.getServiceNameFromAppEngineWebXml(
            testFixture.getProject(), deploymentSource))
        .thenReturn(serviceName);

    Disposer.dispose(editor);
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);

    assertThat(editor.getCommonConfig().getServiceLabel().getText()).isEqualTo(serviceName);
  }

  @Test
  public void newInstance_doesSetEnvironmentLabel() {
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);

    Disposer.dispose(editor);
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);

    assertThat(editor.getCommonConfig().getEnvironmentLabel().getText())
        .isEqualTo(AppEngineEnvironment.APP_ENGINE_STANDARD.localizedLabel());
  }

  @Test
  public void newInstance_withStandardEnvironment_doesHideCostWarningPanel() {
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_STANDARD);

    Disposer.dispose(editor);
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);

    assertThat(editor.getCommonConfig().getAppEngineCostWarningPanel().isVisible()).isFalse();
  }

  @Test
  public void newInstance_withFlexEnvironment_doesHideCostWarningPanel() {
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX);

    Disposer.dispose(editor);
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);

    assertThat(editor.getCommonConfig().getAppEngineCostWarningPanel().isVisible()).isFalse();
  }

  @Test
  public void newInstance_withFlexCompatEnvironment_doesNotHideCostWarningPanel() {
    when(deploymentSource.getEnvironment()).thenReturn(AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT);

    Disposer.dispose(editor);
    editor = new AppEngineStandardDeploymentEditor(testFixture.getProject(), deploymentSource);

    assertThat(editor.getCommonConfig().getAppEngineCostWarningPanel().isVisible()).isTrue();
  }

  @Test
  public void applyEditorTo_withDefaultConfiguration_doesSetDefaults() throws Exception {
    editor.applyEditorTo(configuration);

    assertThat(configuration.getCloudProjectName()).isNull();
    assertThat(configuration.getGoogleUsername()).isNull();
    assertThat(configuration.getEnvironment()).isEqualTo(AppEngineEnvironment.APP_ENGINE_STANDARD);
    assertThat(configuration.getUserSpecifiedArtifactPath()).isNull();
    assertThat(configuration.isPromote()).isFalse();
    assertThat(configuration.isStopPreviousVersion()).isFalse();
    assertThat(configuration.getVersion()).isEmpty();
    assertThat(configuration.isDeployAllConfigs()).isTrue();
    assertThat(configuration.getModuleName()).isNull();
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
    String projectId = "some-project";
    CloudProject project = CloudProject.create(projectId, projectId, EMAIL);
    when(projectSelector.getSelectedProject()).thenReturn(project);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getCloudProjectName()).isEqualTo(projectId);
  }

  @Test
  public void applyEditorTo_doesSetStopPreviousVersion() throws Exception {
    editor.getCommonConfig().getStopPreviousVersionCheckbox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.isStopPreviousVersion()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetDeployAllConfigs() throws Exception {
    editor.getCommonConfig().getDeployAllConfigsCheckbox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.isDeployAllConfigs()).isTrue();
  }

  @Test
  public void applyEditorTo_withUser_doesSetGoogleUsername() throws Exception {
    when(credentialedUser.getEmail()).thenReturn(EMAIL);
    String projectId = "some-project";
    CloudProject project = CloudProject.create(projectId, projectId, EMAIL);
    when(projectSelector.getSelectedProject()).thenReturn(project);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getGoogleUsername()).isEqualTo(EMAIL);
  }

  @Test
  public void applyEditorTo_withFlexCompatSource_doesSetEnvironment() throws Exception {
    when(mockAppEngineProjectService.isFlexCompat(testFixture.getProject(), deploymentSource))
        .thenReturn(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getEnvironment())
        .isEqualTo(AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT);
  }

  @Test
  public void applyEditorTo_doesSetCompileEncoding() throws Exception {
    String encoding = "UTF-8";
    editor.getStagingPanel().getCompileEncodingTextField().setText(encoding);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getCompileEncoding()).isEqualTo(encoding);
  }

  @Test
  public void applyEditorTo_doesSetDeleteJsps() throws Exception {
    editor.getStagingPanel().getDeleteJspsCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getDeleteJsps()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetDisableJarJsps() throws Exception {
    editor.getStagingPanel().getDisableJarJspsCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getDisableJarJsps()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetDisableUpdateCheck() throws Exception {
    editor.getStagingPanel().getDisableUpdateCheckCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getDisableUpdateCheck()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableJarClasses() throws Exception {
    editor.getStagingPanel().getEnableJarClassesCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getEnableJarClasses()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableJarSplitting() throws Exception {
    editor.getStagingPanel().getEnableJarSplittingCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getEnableJarSplitting()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetEnableQuickstart() throws Exception {
    editor.getStagingPanel().getEnableQuickstartCheckBox().setSelected(true);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getEnableQuickstart()).isTrue();
  }

  @Test
  public void applyEditorTo_doesSetJarSplittingExcludes() throws Exception {
    String exclusions = "exclude.this";
    editor.getStagingPanel().getJarSplittingExcludesTextField().setText(exclusions);

    editor.applyEditorTo(configuration);

    assertThat(configuration.getJarSplittingExcludes()).isEqualTo(exclusions);
  }

  @Test
  public void resetEditorFrom_withDefaultConfiguration_doesSetDefaults() {
    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getVersionIdField().getText()).isEmpty();
    assertThat(editor.getCommonConfig().getPromoteCheckbox().isSelected()).isFalse();
    assertThat(editor.getCommonConfig().getProjectSelector().getSelectedProject()).isNull();
    assertThat(editor.getCommonConfig().getStopPreviousVersionCheckbox().isSelected()).isFalse();
    assertThat(editor.getCommonConfig().getDeployAllConfigsCheckbox().isSelected()).isFalse();
    assertThat(editor.getCommonConfig().getEnvironmentLabel().getText()).isEmpty();
  }

  @Test
  public void resetEditorFrom_doesSetVersion() {
    String version = "some-version;";
    configuration.setVersion(version);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getVersionIdField().getText()).isEqualTo(version);
  }

  @Test
  public void resetEditorFrom_doesSetPromote() {
    configuration.setPromote(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getPromoteCheckbox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetStopPreviousVersion() {
    configuration.setStopPreviousVersion(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getStopPreviousVersionCheckbox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetDeployAllConfigs() {
    configuration.setDeployAllConfigs(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getDeployAllConfigsCheckbox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnvironment() {
    AppEngineEnvironment environment = AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT;
    configuration.setEnvironment(environment);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getCommonConfig().getEnvironmentLabel().getText())
        .isEqualTo(environment.localizedLabel());
  }

  @Test
  public void resetEditorFrom_doesSet_matchingCloudProject() {
    String projectId = "some-project";
    configuration.setCloudProjectName(projectId);
    configuration.setGoogleUsername(EMAIL);

    editor.resetEditorFrom(configuration);

    CloudProject expectedProject = CloudProject.create(projectId, projectId, EMAIL);
    verify(projectSelector).setSelectedProject(expectedProject);
  }

  @Test
  public void resetEditorFrom_doesSetCompileEncoding() {
    String encoding = "UTF-8";
    configuration.setCompileEncoding(encoding);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getCompileEncodingTextField().getText())
        .isEqualTo(encoding);
  }

  @Test
  public void resetEditorFrom_doesSetDeleteJsps() {
    configuration.setDeleteJsps(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getDeleteJspsCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetDisableJarJsps() {
    configuration.setDisableJarJsps(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getDisableJarJspsCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetDisableUpdateCheck() {
    configuration.setDisableUpdateCheck(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getDisableUpdateCheckCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableJarClasses() {
    configuration.setEnableJarClasses(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getEnableJarClassesCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableJarSplitting() {
    configuration.setEnableJarSplitting(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getEnableJarSplittingCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetEnableQuickstart() {
    configuration.setEnableQuickstart(true);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getEnableQuickstartCheckBox().isSelected()).isTrue();
  }

  @Test
  public void resetEditorFrom_doesSetJarSplittingExcludes() {
    String exclusions = "exclude.this";
    configuration.setJarSplittingExcludes(exclusions);

    editor.resetEditorFrom(configuration);

    assertThat(editor.getStagingPanel().getJarSplittingExcludesTextField().getText())
        .isEqualTo(exclusions);
  }

  @Test
  public void parametersTabbedPane_hasTwoTabs() {
    JTabbedPane parametersTabbedPane = editor.getCommonConfig().getParametersTabbedPane();
    assertThat(parametersTabbedPane.getTabCount()).isEqualTo(2);
    assertThat(parametersTabbedPane.getTitleAt(0))
        .isEqualTo(GctBundle.message("appengine.deployment.parameters.tab.title"));
    assertThat(parametersTabbedPane.getTitleAt(1))
        .isEqualTo(GctBundle.message("appengine.deployment.staging.properties.tab.title"));
  }
}
