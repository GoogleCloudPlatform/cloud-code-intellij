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

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineArtifactDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.MavenBuildDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.UserSpecifiedPathDeploymentSource;
import com.google.common.collect.Lists;

import com.intellij.facet.Facet;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * App Engine utility methods.
 */
public class AppEngineUtil {

  private static final String APP_ENGINE_STANDARD_FACET_NAME = "Google App Engine";

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  /**
   * Creates a list of artifact deployment sources available for deployment to App Engine.
   *
   * <p>Artifacts either target the standard or the flexible environment. All standard artifacts are
   * added. Flexible artifacts are only added if there are no other standard artifacts associated
   * with the same module - i.e. if a module is set up for App Engine standard (has an
   * appengine-web.xml etc.) then no option is provided to deploy any of its artifacts to the
   * flexible environment.
   *
   * @return a list of {@link AppEngineArtifactDeploymentSource}'s
   */
  public static List<AppEngineArtifactDeploymentSource> createArtifactDeploymentSources(
      @NotNull Project project) {
    List<AppEngineArtifactDeploymentSource> sources = Lists.newArrayList();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
      boolean addFlexArtifact = !containsAppEngineStandardArtifacts(project, artifacts);

      for (Artifact artifact : artifacts) {
        AppEngineEnvironment environment = getAppEngineArtifactEnvironment(project, artifact);

        if (environment != null
            && environment == AppEngineEnvironment.APP_ENGINE_STANDARD
            || (environment == AppEngineEnvironment.APP_ENGINE_FLEX && addFlexArtifact)) {
          sources.add(createArtifactDeploymentSource(project, artifact, environment));
        }
      }
    }

    return sources;
  }

  /**
   * Creates a list of module deployment sources available for deployment to App Engine. Currently,
   * all module based sources target the App Engine flexible environment:
   *
   * <p>Maven based deployment sources are included if there are no App Engine standard artifacts
   * associated with the same module.
   *
   * <p>User browsable jar/war deployment sources are always available.
   *
   * @return a list of {@link ModuleDeploymentSource}'s
   */
  public static List<ModuleDeploymentSource> createModuleDeploymentSources(
      @NotNull Project project) {
    List<ModuleDeploymentSource> moduleDeploymentSources = Lists.newArrayList();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (ModuleType.is(module, JavaModuleType.getModuleType())
          && !containsAppEngineStandardArtifacts(project, module)
          && isJarOrWarMavenBuild(project, module)) {
        moduleDeploymentSources.add(createMavenBuildDeploymentSource(project, module));
      }
    }

    // I kind of expected the logic for adding the user specific deployment source to be in the
    // artifact based deployment sources.
    moduleDeploymentSources.add(createUserSpecifiedPathDeploymentSource(project));

    return moduleDeploymentSources;
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
      @NotNull Project project,
      @NotNull Module module) {
    return new MavenBuildDeploymentSource(
        ModulePointerManager.getInstance(project).create(module), project);
  }

  private static UserSpecifiedPathDeploymentSource createUserSpecifiedPathDeploymentSource(
      @NotNull Project project) {
    ModulePointer modulePointer =
        ModulePointerManager.getInstance(project).create("userSpecifiedSource");

    return new UserSpecifiedPathDeploymentSource(modulePointer);
  }

  @Nullable
  private static AppEngineEnvironment getAppEngineArtifactEnvironment(@NotNull Project project,
      @NotNull Artifact artifact) {
    if (hasAppEngineStandardFacet(project, artifact) && isAppEngineStandardArtifactType(artifact)) {
      return AppEngineEnvironment.APP_ENGINE_STANDARD;
    } else if (isAppEngineFlexArtifactType(artifact)) {
      return AppEngineEnvironment.APP_ENGINE_FLEX;
    } else {
      return null;
    }
  }

  private static boolean containsAppEngineStandardArtifacts(@NotNull Project project,
      @NotNull Module module) {
    return containsAppEngineStandardArtifacts(
        project, ArtifactUtil.getArtifactsContainingModuleOutput(module));
  }

  private static boolean containsAppEngineStandardArtifacts(
      Project project, Collection<Artifact> artifacts) {
    for (Artifact artifact : artifacts) {
      if (hasAppEngineStandardFacet(project, artifact)
          && isAppEngineStandardArtifactType(artifact)) {
        return true;
      }
    }

    return false;
  }

  /**
   * An artifact has an app engine standard facet if it associated with a module that has a facet
   * whose name matches that of the facet configured by the App Engine legacy IJ plugin.
   */
  private static boolean hasAppEngineStandardFacet(@NotNull Project project,
      @NotNull Artifact artifact) {
    Set<Module> modules = ArtifactUtil
        .getModulesIncludedInArtifacts(Collections.singletonList(artifact), project);

    for (Module module : modules) {
      for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
        if (facet != null && APP_ENGINE_STANDARD_FACET_NAME.equals(facet.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isAppEngineStandardArtifactType(@NotNull Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "exploded-war".equalsIgnoreCase(artifactId);
  }

  private static boolean isAppEngineFlexArtifactType(@NotNull Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "jar".equalsIgnoreCase(artifactId) || "war".equals(artifactId);
  }

  private static boolean isJarOrWarMavenBuild(@NotNull Project project, @NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
    MavenProject mavenProject = projectsManager.findProject(module);

    boolean isMavenProject = projectsManager.isMavenizedModule(module)
        && mavenProject != null;

    return isMavenProject
        && ("jar".equalsIgnoreCase(mavenProject.getPackaging())
        || "war".equalsIgnoreCase(mavenProject.getPackaging()));

  }
}
