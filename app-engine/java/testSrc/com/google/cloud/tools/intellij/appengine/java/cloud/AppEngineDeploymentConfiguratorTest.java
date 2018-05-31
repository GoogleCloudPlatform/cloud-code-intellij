/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import static com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment.APP_ENGINE_FLEX;
import static com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment.APP_ENGINE_STANDARD;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.cloud.flexible.AppEngineFlexibleDeploymentEditor;
import com.google.cloud.tools.intellij.appengine.java.cloud.standard.AppEngineStandardDeploymentEditor;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineGradlePluginFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestScopedSystemPropertyRule;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.util.PlatformUtils;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Tests for {@link AppEngineDeploymentConfigurator}. */
@RunWith(JUnit4.class)
public final class AppEngineDeploymentConfiguratorTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @Rule
  public final TestScopedSystemPropertyRule systemPropertyRule =
      new TestScopedSystemPropertyRule(PlatformUtils.PLATFORM_PREFIX_KEY);

  @Mock private AppEngineDeployable mockAppEngineDeployable;
  @Mock private DeploymentSource mockDeploymentSource;
  @Mock private RemoteServer<AppEngineServerConfiguration> mockRemoteServer;

  @TestModule private Module gradleModule;

  private AppEngineDeploymentConfigurator configurator;

  @Before
  public void setUpConfigurator() {
    configurator = new AppEngineDeploymentConfigurator(testFixture.getProject());
  }

  @After
  public void tearDown() throws IllegalAccessException {
    // scratch project reference to avoid project leaks.
    UsefulTestCase.clearDeclaredFields(configurator, configurator.getClass());
  }

  @Test
  public void createDefaultConfiguration_withFlexibleEnvironment_doesReturnDefaultConfig() {
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(APP_ENGINE_FLEX);

    AppEngineDeploymentConfiguration configuration =
        configurator.createDefaultConfiguration(mockAppEngineDeployable);

    assertThat(configuration).isEqualTo(new AppEngineDeploymentConfiguration());
  }

  @Test
  public void createDefaultConfiguration_withDifferentDeploymentSource_doesReturnDefaultConfig() {
    AppEngineDeploymentConfiguration configuration =
        configurator.createDefaultConfiguration(mockDeploymentSource);

    assertThat(configuration).isEqualTo(new AppEngineDeploymentConfiguration());
  }

  @Test
  public void createEditor_withFlexibleEnvironment_doesReturnFlexibleEditor() {
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(APP_ENGINE_FLEX);

    SettingsEditor<AppEngineDeploymentConfiguration> editor =
        configurator.createEditor(mockAppEngineDeployable, mockRemoteServer);

    assertThat(editor).isInstanceOf(AppEngineFlexibleDeploymentEditor.class);
  }

  @Test
  public void createEditor_withStandardEnvironment_doesReturnStandardEditor() {
    when(mockAppEngineDeployable.getEnvironment()).thenReturn(APP_ENGINE_STANDARD);

    SettingsEditor<AppEngineDeploymentConfiguration> editor =
        configurator.createEditor(mockAppEngineDeployable, mockRemoteServer);

    assertThat(editor).isInstanceOf(AppEngineStandardDeploymentEditor.class);
  }

  @Test
  public void getDeploymentSources_withCommunityEdition_returnsSource() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);

    addAppEngineAndGradleFacets();

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              List<DeploymentSource> availableDeploymentSources =
                  configurator.getAvailableDeploymentSources();

              assertThat(availableDeploymentSources).hasSize(1);
              assertThat(availableDeploymentSources.get(0))
                  .isInstanceOf(GradlePluginDeploymentSource.class);
            });
  }

  @Test
  public void getDeploymentSources_withUltimateEdition_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_PREFIX);

    addAppEngineAndGradleFacets();

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              List<DeploymentSource> availableDeploymentSources =
                  configurator.getAvailableDeploymentSources();

              assertThat(availableDeploymentSources).isEmpty();
            });
  }

  private void addAppEngineAndGradleFacets() {
    ModuleTestUtils.addFacet(gradleModule, AppEngineStandardFacetType.ID);
    ModuleTestUtils.addFacet(gradleModule, AppEngineGradlePluginFacetType.ID);
  }
}
