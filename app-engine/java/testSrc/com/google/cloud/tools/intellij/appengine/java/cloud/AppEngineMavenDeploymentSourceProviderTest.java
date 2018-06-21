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
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.MavenTestUtils;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.List;
import java.util.Optional;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link AppEngineMavenDeploymentSourceProvider}. */
public class AppEngineMavenDeploymentSourceProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  private @TestService @Mock AppEngineProjectService projectService;

  private AppEngineMavenDeploymentSourceProvider mavenDeploymentSourceProvider;
  private MavenModuleBuilder mavenModuleBuilder;

  @Before
  public void setUp() {
    mavenDeploymentSourceProvider = new AppEngineMavenDeploymentSourceProvider();
    mavenModuleBuilder =
        MavenTestUtils.getInstance().initMavenModuleBuilder(testFixture.getProject());
  }

  @Test
  public void getSources_withNoMavenizedModules_returnsEmptySources() {
    List<DeploymentSource> mavenSources = addStandardFacetAndReturnSources(module);

    assertThat(mavenSources).isEmpty();
  }

  @Test
  public void getSources_withMavenizedModule_returnsMavenSources() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              try {
                ApplicationManager.getApplication()
                    .runWriteAction(
                        () -> {
                          Module mavenModule =
                              MavenTestUtils.getInstance()
                                  .createNewMavenModule(
                                      mavenModuleBuilder, testFixture.getProject());

                          when(projectService.isJarOrWarMavenBuild(mavenModule)).thenReturn(true);

                          List<DeploymentSource> mavenSources =
                              addStandardFacetAndReturnSources(mavenModule);

                          assertThat(mavenSources.size()).isEqualTo(1);
                          assertThat(mavenSources.get(0))
                              .isInstanceOf(MavenBuildDeploymentSource.class);
                        });
              } finally {
                MavenServerManager.getInstance().shutdown(true);
              }
            });
  }

  @Test
  public void getSources_withMavenizedModule_andNoAppEngineModules_returnsEmptySources() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              try {
                ApplicationManager.getApplication()
                    .runWriteAction(
                        () -> {
                          Module mavenModule =
                              MavenTestUtils.getInstance()
                                  .createNewMavenModule(
                                      mavenModuleBuilder, testFixture.getProject());

                          when(projectService.isJarOrWarMavenBuild(mavenModule)).thenReturn(true);

                          List<DeploymentSource> mavenSources =
                              mavenDeploymentSourceProvider.getDeploymentSources(
                                  testFixture.getProject());

                          assertThat(mavenSources).isEmpty();
                        });
              } finally {
                MavenServerManager.getInstance().shutdown(true);
              }
            });
  }

  private List<DeploymentSource> addStandardFacetAndReturnSources(Module targetModule) {
    ModuleTestUtils.addFacet(targetModule, AppEngineStandardFacetType.ID);
    when(projectService.getModuleAppEngineEnvironment(targetModule))
        .thenReturn(Optional.of(AppEngineEnvironment.APP_ENGINE_STANDARD));

    return mavenDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());
  }
}
