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
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
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
import com.intellij.psi.xml.XmlFile;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
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
      @NotNull final Project project) {
    List<AppEngineArtifactDeploymentSource> sources = Lists.newArrayList();
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();
    AppEngineAssetProvider assetProvider = AppEngineAssetProvider.getInstance();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      XmlFile appEngineWebXml
          = assetProvider.loadAppEngineStandardWebXml(project, Collections.singletonList(module));
      final AppEngineEnvironment environment
          = projectService.getModuleAppEngineEnvironment(appEngineWebXml);

      boolean isFlexCompat = projectService.isFlexCompat(appEngineWebXml);
      boolean isStandardModule = environment.isStandard() || isFlexCompat;

      Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
      for (Artifact artifact : artifacts) {
        if ((isStandardModule && projectService.isAppEngineStandardArtifactType(artifact))
            || (environment.isFlexible() && projectService.isAppEngineFlexArtifactType(artifact))) {
          sources.add(createArtifactDeploymentSource(project, artifact, environment));
        }
      }
    }

    return sources;
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
    AppEngineAssetProvider assetProvider = AppEngineAssetProvider.getInstance();

    List<ModuleDeploymentSource> moduleDeploymentSources = Lists.newArrayList();

    boolean hasStandardModules = false;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      AppEngineEnvironment environment =
          projectService.getModuleAppEngineEnvironment(
              assetProvider.loadAppEngineStandardWebXml(
                  project, Collections.singletonList(module)));

      if (ModuleType.is(module, JavaModuleType.getModuleType())
          && projectService.isJarOrWarMavenBuild(project, module)) {
        moduleDeploymentSources.add(createMavenBuildDeploymentSource(project, module, environment));
      }

      if (environment == AppEngineEnvironment.APP_ENGINE_STANDARD) {
        hasStandardModules = true;
      }
    }

    if (!hasStandardModules) {
      moduleDeploymentSources.add(createUserSpecifiedPathDeploymentSource(project));
    }

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
      @NotNull Module module,
      @NotNull AppEngineEnvironment environment) {
    return new MavenBuildDeploymentSource(
        ModulePointerManager.getInstance(project).create(module), project, environment);
  }

  private static UserSpecifiedPathDeploymentSource createUserSpecifiedPathDeploymentSource(
      @NotNull Project project) {
    ModulePointer modulePointer =
        ModulePointerManager.getInstance(project).create("userSpecifiedSource");

    return new UserSpecifiedPathDeploymentSource(modulePointer);
  }
}
