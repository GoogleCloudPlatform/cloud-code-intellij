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

package com.google.cloud.tools.intellij.appengine.java.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineGradlePluginFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineUtil}. */
public class AppEngineUtilTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  @Test
  public void createGradleSource_withAppEngineFacets_returnsGradleSource() {
    ModuleTestUtils.addFacet(module, AppEngineStandardFacetType.ID);
    ModuleTestUtils.addFacet(module, AppEngineGradlePluginFacetType.ID);

    List<ModuleDeploymentSource> sources =
        AppEngineUtil.createGradlePluginDeploymentSources(testFixture.getProject());

    assertThat(sources).hasSize(1);
  }

  @Test
  public void createGradleSource_withNoAppEngineFacets_returnsEmpty() {
    List<ModuleDeploymentSource> sources =
        AppEngineUtil.createGradlePluginDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void createGradleSource_withAppEngineFacet_andNoAppEngineGradleFacet_returnsEmpty() {
    ModuleTestUtils.addFacet(module, AppEngineStandardFacetType.ID);

    List<ModuleDeploymentSource> sources =
        AppEngineUtil.createGradlePluginDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }

  @Test
  public void createGradleSource_withAppEngineGradleFacet_andNoAppEngineFacet_returnsEmpty() {
    ModuleTestUtils.addFacet(module, AppEngineGradlePluginFacetType.ID);

    List<ModuleDeploymentSource> sources =
        AppEngineUtil.createGradlePluginDeploymentSources(testFixture.getProject());

    assertThat(sources).isEmpty();
  }
}
