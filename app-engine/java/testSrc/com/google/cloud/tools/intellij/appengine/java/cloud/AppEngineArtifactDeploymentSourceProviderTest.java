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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.AppEngineArtifactDeploymentSourceProvider;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.ArtifactManagerImpl;
import com.intellij.packaging.impl.artifacts.PlainArtifactType;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class AppEngineArtifactDeploymentSourceProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module1;
  @TestModule private Module module2;

  @TestService @Mock AppEngineProjectService projectService;

  private AppEngineArtifactDeploymentSourceProvider artifactDeploymentSourceProvider;

  @Before
  public void setUp() {
    artifactDeploymentSourceProvider = new AppEngineArtifactDeploymentSourceProvider();
  }

  @Test
  public void getDeploymentSources_withOnlyStandardArtifacts_returnsAllSources() {
    makeStandard(module1);
    makeStandard(module2);

    assertDeploymentSourcesPresent(AppEngineEnvironment.APP_ENGINE_STANDARD);
  }

  @Test
  public void getDeploymentSources_withOnlyFlexibleArtifacts_returnsAllSources() {
    makeFlexible(module1);
    makeFlexible(module2);

    assertDeploymentSourcesPresent(AppEngineEnvironment.APP_ENGINE_FLEX);
  }

  @Test
  public void getDeploymentSources_withStandardAndFlexibleArtifacts_returnsAllSources() {
    makeStandard(module1);
    makeFlexible(module2);

    List<DeploymentSource> deploymentSources =
        artifactDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(deploymentSources.size()).isEqualTo(2);

    DeploymentSource deploymentSource1 = deploymentSources.get(0);
    DeploymentSource deploymentSource2 = deploymentSources.get(1);

    assertThat(deploymentSource1 instanceof AppEngineArtifactDeploymentSource).isTrue();
    assertThat(deploymentSource2 instanceof AppEngineArtifactDeploymentSource).isTrue();
    assertThat(((AppEngineArtifactDeploymentSource) deploymentSource1).getEnvironment())
        .isEqualTo(AppEngineEnvironment.APP_ENGINE_STANDARD);
    assertThat(((AppEngineArtifactDeploymentSource) deploymentSource2).getEnvironment())
        .isEqualTo(AppEngineEnvironment.APP_ENGINE_FLEX);
  }

  private void assertDeploymentSourcesPresent(AppEngineEnvironment environment) {
    List<DeploymentSource> deploymentSources =
        artifactDeploymentSourceProvider.getDeploymentSources(testFixture.getProject());

    assertThat(deploymentSources.size()).isEqualTo(2);

    DeploymentSource deploymentSource1 = deploymentSources.get(0);
    DeploymentSource deploymentSource2 = deploymentSources.get(1);

    assertThat(deploymentSource1 instanceof AppEngineArtifactDeploymentSource).isTrue();
    assertThat(deploymentSource2 instanceof AppEngineArtifactDeploymentSource).isTrue();
    assertThat(((AppEngineArtifactDeploymentSource) deploymentSource1).getEnvironment())
        .isEqualTo(environment);
    assertThat(((AppEngineArtifactDeploymentSource) deploymentSource2).getEnvironment())
        .isEqualTo(environment);
  }

  /**
   * Adds the {@link AppEngineStandardFacet} to the module, associates the module with the standard
   * environment, and creates a new artifact associated with the module output.
   *
   * @param module
   */
  private void makeStandard(Module module) {
    ModuleTestUtils.addFacet(module, AppEngineStandardFacetType.ID);

    when(projectService.getModuleAppEngineEnvironment(module))
        .thenReturn(Optional.of(AppEngineEnvironment.APP_ENGINE_STANDARD));
    when(projectService.isAppEngineStandardArtifactType(any())).thenReturn(true);

    addArtifact(module);
  }

  /**
   * Adds the {@link AppEngineFlexibleFacet} to the module, associates the module with the flexible
   * environment, and creates a new artifact associated with the module output.
   *
   * @param module
   */
  private void makeFlexible(Module module) {
    ModuleTestUtils.addFacet(module, AppEngineFlexibleFacetType.ID);

    when(projectService.getModuleAppEngineEnvironment(module))
        .thenReturn(Optional.of(AppEngineEnvironment.APP_ENGINE_FLEX));
    when(projectService.isAppEngineFlexArtifactType(any())).thenReturn(true);

    addArtifact(module);
  }

  private void addArtifact(Module module) {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                ApplicationManager.getApplication()
                    .runWriteAction(
                        () -> {
                          ArtifactManager artifactManager =
                              ArtifactManagerImpl.getInstance(testFixture.getProject());
                          ModifiableArtifactModel modifiableArtifactModel =
                              artifactManager.createModifiableModel();
                          ArchivePackagingElement archivePackagingElement =
                              new ArchivePackagingElement(module.getName());
                          final PackagingElement<?> moduleOutput =
                              PackagingElementFactory.getInstance().createModuleOutput(module);
                          final ModifiableArtifact artifact =
                              modifiableArtifactModel.addArtifact(
                                  module.getName(),
                                  PlainArtifactType.getInstance(),
                                  archivePackagingElement);
                          PackagingElementFactory.getInstance()
                              .getOrCreateArchive(artifact.getRootElement(), module.getName())
                              .addFirstChild(moduleOutput);

                          modifiableArtifactModel.commit();
                        }));
  }
}
