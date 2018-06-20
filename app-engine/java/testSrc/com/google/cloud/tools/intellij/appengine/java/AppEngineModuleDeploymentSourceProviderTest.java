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

package com.google.cloud.tools.intellij.appengine.java;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineModuleDeploymentSourceProvider;
import com.google.cloud.tools.intellij.appengine.java.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link AppEngineModuleDeploymentSourceProvider}. */
public class AppEngineModuleDeploymentSourceProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module1;
  @TestModule private Module module2;

  @TestService @Mock AppEngineProjectService projectService;

  private AppEngineModuleDeploymentSourceProvider moduleDeploymentSourceProvider;

  @Before
  public void setUp() {
    moduleDeploymentSourceProvider = new AppEngineModuleDeploymentSourceProvider();
  }

  @Test
  public void getDeploymentSources_withAppEngineStandardModules_doesNotReturnUserSpecifiedSource() {
    ModuleTestUtils.addFacet(module1, AppEngineStandardFacetType.ID);
    when(projectService.getModuleAppEngineEnvironment(module1))
        .thenReturn(Optional.of(AppEngineEnvironment.APP_ENGINE_STANDARD));

    List<DeploymentSource> deploymentSources =
        moduleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(deploymentSources).isEmpty();
  }

  @Test
  public void getDeploymentSources_withNoAppEngineStandardModules_returnsUserSpecifiedSource() {
    ModuleTestUtils.addFacet(module1, AppEngineFlexibleFacetType.ID);
    ModuleTestUtils.addFacet(module2, AppEngineFlexibleFacetType.ID);

    when(projectService.getModuleAppEngineEnvironment(any()))
        .thenReturn(Optional.of(AppEngineEnvironment.APP_ENGINE_FLEX));

    List<DeploymentSource> deploymentSources =
        moduleDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(deploymentSources.size()).isEqualTo(1);
    assertThat(deploymentSources.get(0) instanceof UserSpecifiedPathDeploymentSource).isTrue();
  }
}
