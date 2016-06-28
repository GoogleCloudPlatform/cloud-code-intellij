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
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.cloud.MavenBuildDeploymentSource;
import com.google.cloud.tools.intellij.appengine.cloud.UserSpecifiedPathDeploymentSource;
import com.google.common.collect.Lists;

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

import java.util.Collection;
import java.util.List;

/**
 * App Engine utility methods.
 */
public class AppEngineUtil {

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
    AppEngineProjectService aeProjectHelper = AppEngineProjectService.getInstance();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);

      for (Artifact artifact : artifacts) {
        boolean hasStandardArtifacts
            = aeProjectHelper.containsAppEngineStandardArtifacts(project, artifacts);
        boolean addFlexArtifact
            = !hasStandardArtifacts || aeProjectHelper.isFlexCompat(project, artifact);
        AppEngineEnvironment environment
            = aeProjectHelper.getAppEngineArtifactEnvironment(project, artifact);

        if (environment != null
            && (environment.isStandard()
            || (environment.isFlexible() && addFlexArtifact))) {
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
    AppEngineProjectService aeProjectHelper = AppEngineProjectService.getInstance();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (ModuleType.is(module, JavaModuleType.getModuleType())
          && !aeProjectHelper.containsAppEngineStandardArtifacts(project, module)
          && aeProjectHelper.isJarOrWarMavenBuild(project, module)) {
        moduleDeploymentSources.add(createMavenBuildDeploymentSource(project, module));
      }
    }

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
}
