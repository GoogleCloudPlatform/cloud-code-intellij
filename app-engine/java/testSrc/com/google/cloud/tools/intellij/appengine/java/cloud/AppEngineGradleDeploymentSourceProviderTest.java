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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestScopedSystemPropertyRule;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.util.PlatformUtils;
import java.util.List;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineGradleDeploymentSourceProvider}. */
public class AppEngineGradleDeploymentSourceProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Rule
  public final TestScopedSystemPropertyRule systemPropertyRule =
      new TestScopedSystemPropertyRule(PlatformUtils.PLATFORM_PREFIX_KEY);

  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  private AppEngineGradleDeploymentSourceProvider gradleDeploymentSourceProvider;

  @Before
  public void setUp() {
    gradleDeploymentSourceProvider = new AppEngineGradleDeploymentSourceProvider();
  }

  @Test
  public void createGradleSource_withAppEngineFacet_andGradleBuildDir_returnsGradleSource() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
    initForGradleSources();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).hasSize(1);
    assertThat(sources.get(0) instanceof GradlePluginDeploymentSource).isTrue();
  }

  @Test
  public void createGradleSource_withAppEngineFacet_andGradleBuildDir_withNoGradle_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
    addGradleBuildDir();
    addAppEngineStandardFacet();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void createGradleSource_withNoAppEngineFacet_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
    addGradleBuildDir();
    enableGradle();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void createGradleSource_withAppEngineFacet_andNoGradleBuildDir_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);
    addAppEngineStandardFacet();
    enableGradle();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void getDeploymentSources_withPyCharm_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.PYCHARM_CE_PREFIX);
    initForGradleSources();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void getDeploymentSources_withUltimateEdition_returnsEmpty() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_PREFIX);
    initForGradleSources();

    List<DeploymentSource> sources =
        gradleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  /**
   * Sets up everything needed for Gradle sources to be collected: adds a gradle build directory,
   * adds the App Engine standard facet, and enables the Gradle external build system.
   */
  private void initForGradleSources() {
    addGradleBuildDir();
    addAppEngineStandardFacet();
    enableGradle();
  }

  private void addGradleBuildDir() {
    AppEngineStandardGradleModuleComponent.getInstance(module)
        .setGradleBuildDir("/path/to/gradle/build");
  }

  private void addAppEngineStandardFacet() {
    ModuleTestUtils.addFacet(module, AppEngineStandardFacetType.ID);
  }

  private void enableGradle() {
    ExternalSystemModulePropertyManager.getInstance(module)
        .setExternalId(GradleConstants.SYSTEM_ID);
  }
}
