/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.util;

import static java.util.stream.Collectors.toList;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.MavenBuildDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.common.collect.Lists;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** App Engine utility methods. */
public class AppEngineUtil {

  public static final String APP_ENGINE_WEB_XML_NAME = "appengine-web.xml";

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  /**
   * Creates a list of artifact deployment sources available for deployment to App Engine.
   *
   * <p>Artifacts either target the standard or the flexible environment. All standard artifacts are
   * added. Flexible artifacts are only added if there are no other standard artifacts associated
   * with the same module.
   *
   * @return a list of {@link AppEngineArtifactDeploymentSource}
   */
  public static List<AppEngineArtifactDeploymentSource> createArtifactDeploymentSources(
      @NotNull final Project project) {
    List<AppEngineArtifactDeploymentSource> sources = Lists.newArrayList();
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
                .map(
                    artifact ->
                        AppEngineUtil.createArtifactDeploymentSource(
                            project, artifact, environment))
                .collect(toList()));
      }
    }

    return sources;
  }

  private static boolean doesArtifactMatchEnvironment(
      Artifact artifact, AppEngineEnvironment environment) {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    return ((environment.isStandard() || environment.isFlexCompat())
            && projectService.isAppEngineStandardArtifactType(artifact))
        || (environment.isFlexible() && projectService.isAppEngineFlexArtifactType(artifact));
  }

  /**
   * Creates a list of module deployment sources available for deployment to App Engine:
   *
   * <p>Maven based deployment sources are included for both flexible and standard projects if
   * applicable.
   *
   * <p>User browsable jar/war deployment sources are included only if there are no App Engine
   * standard modules.
   *
   * @return a list of {@link ModuleDeploymentSource}'s
   */
  public static List<ModuleDeploymentSource> createModuleDeploymentSources(
      @NotNull Project project) {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    List<ModuleDeploymentSource> moduleDeploymentSources = Lists.newArrayList();

    boolean hasStandardModules = false;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      FacetManager facetManager = FacetManager.getInstance(module);
      if (facetManager.getFacetByType(AppEngineStandardFacetType.ID) != null
          || facetManager.getFacetByType(AppEngineFlexibleFacetType.ID) != null) {
        AppEngineEnvironment environment =
            projectService
                .getModuleAppEngineEnvironment(module)
                .orElseThrow(() -> new RuntimeException("No environment."));

        if (ModuleType.is(module, JavaModuleType.getModuleType())
            && projectService.isJarOrWarMavenBuild(module)) {
          moduleDeploymentSources.add(
              createMavenBuildDeploymentSource(project, module, environment));
        }

        if (environment.isStandard() || environment.isFlexCompat()) {
          hasStandardModules = true;
        }
      }
    }

    if (!hasStandardModules) {
      moduleDeploymentSources.add(createUserSpecifiedPathDeploymentSource(project));
    }

    return moduleDeploymentSources;
  }

  /**
   * Returns the only app engine standard artifact found for the given module or null if there
   * aren't any or more than one.
   */
  @Nullable
  public static Artifact findOneAppEngineStandardArtifact(@NotNull Module module) {
    Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
    Collection<Artifact> appEngineStandardArtifacts = Lists.newArrayList();
    appEngineStandardArtifacts.addAll(
        artifacts
            .stream()
            .filter(
                artifact ->
                    AppEngineProjectService.getInstance().isAppEngineStandardArtifactType(artifact))
            .collect(toList()));

    return appEngineStandardArtifacts.size() == 1
        ? appEngineStandardArtifacts.iterator().next()
        : null;
  }

  private static AppEngineArtifactDeploymentSource createArtifactDeploymentSource(
      @NotNull Project project,
      @NotNull Artifact artifact,
      @NotNull AppEngineEnvironment environment) {
    ArtifactPointerManager pointerManager = ArtifactPointerManager.getInstance(project);

    return new AppEngineArtifactDeploymentSource(
        environment, pointerManager.createPointer(artifact));
  }

  private static MavenBuildDeploymentSource createMavenBuildDeploymentSource(
      @NotNull Project project, @NotNull Module module, @NotNull AppEngineEnvironment environment) {
    return new MavenBuildDeploymentSource(
        ModulePointerManager.getInstance(project).create(module), project, environment);
  }

  private static UserSpecifiedPathDeploymentSource createUserSpecifiedPathDeploymentSource(
      @NotNull Project project) {
    ModulePointer modulePointer =
        ModulePointerManager.getInstance(project)
            .create(UserSpecifiedPathDeploymentSource.moduleName);

    return new UserSpecifiedPathDeploymentSource(modulePointer);
  }
}
