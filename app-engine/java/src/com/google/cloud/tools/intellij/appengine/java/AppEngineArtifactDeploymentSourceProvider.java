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

import static java.util.stream.Collectors.toList;

import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploymentSourceProvider;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.common.collect.Lists;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** An {@link AppEngineDeploymentSourceProvider} that collects artifact-based deployment sources. */
public class AppEngineArtifactDeploymentSourceProvider
    implements AppEngineDeploymentSourceProvider {

  /**
   * Collects a list of artifact deployment sources available for deployment to App Engine.
   *
   * <p>Artifacts either target the standard or the flexible environment. All standard artifacts are
   * added. Flexible artifacts are only added if there are no other standard artifacts associated
   * with the same module.
   *
   * @return a list of {@link AppEngineArtifactDeploymentSource}
   */
  @Override
  public List<DeploymentSource> getDeploymentSources(@NotNull Project project) {
    List<DeploymentSource> sources = Lists.newArrayList();
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      FacetManager facetManager = FacetManager.getInstance(module);
      if (facetManager.getFacetByType(AppEngineStandardFacetType.ID) != null
          || facetManager.getFacetByType(AppEngineFlexibleFacetType.ID) != null) {
        final AppEngineEnvironment environment =
            projectService
                .getModuleAppEngineEnvironment(module)
                .orElseThrow(() -> new RuntimeException("No environment."));

        Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
        sources.addAll(
            artifacts
                .stream()
                .filter(artifact -> doesArtifactMatchEnvironment(artifact, environment))
                .map(artifact -> createArtifactDeploymentSource(project, artifact, environment))
                .collect(toList()));
      }
    }

    return sources;
  }

  /** Instantiates a new {@link AppEngineArtifactDeploymentSource}. */
  private static AppEngineArtifactDeploymentSource createArtifactDeploymentSource(
      @NotNull Project project,
      @NotNull Artifact artifact,
      @NotNull AppEngineEnvironment environment) {
    ArtifactPointerManager pointerManager = ArtifactPointerManager.getInstance(project);

    return new AppEngineArtifactDeploymentSource(
        environment, pointerManager.createPointer(artifact));
  }

  /**
   * Returns {@code true} if the supplied {@link Artifact} is compatible with the supplied {@link
   * AppEngineEnvironment}. Returns {@code false} otherwise.
   */
  private static boolean doesArtifactMatchEnvironment(
      Artifact artifact, AppEngineEnvironment environment) {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    return ((environment.isStandard() || environment.isFlexCompat())
            && projectService.isAppEngineStandardArtifactType(artifact))
        || (environment.isFlexible() && projectService.isAppEngineFlexArtifactType(artifact));
  }
}
