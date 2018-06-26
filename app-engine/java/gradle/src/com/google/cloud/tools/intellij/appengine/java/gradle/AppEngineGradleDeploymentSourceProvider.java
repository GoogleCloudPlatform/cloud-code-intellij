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

package com.google.cloud.tools.intellij.appengine.java.gradle;

import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploymentSourceProvider;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.google.cloud.tools.intellij.appengine.java.gradle.project.GradleProjectService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.util.PlatformUtils;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/** An {@link AppEngineDeploymentSourceProvider} that collects Gradle-based deployment sources. */
public class AppEngineGradleDeploymentSourceProvider implements AppEngineDeploymentSourceProvider {

  /**
   * Assembles a list of {@link GradlePluginDeploymentSource} for each module.
   *
   * <p>Will create a Gradle plugin deployment source if the module has both the App Engine standard
   * the App Engine Gradle plugin facet.
   *
   * <p>Will only collect deployment sources if the environment is IDEA Community. This is because
   * Gradle-based deployment sources work natively via artifact deployment sources with IDEA
   * Ultimate making {@link GradlePluginDeploymentSource} unnecessary.
   *
   * @param project the current {@link Project}
   * @return a list of {@link GradlePluginDeploymentSource} containing the app-gradle-plugin based
   *     deployment sources.
   */
  @Override
  public List<DeploymentSource> getDeploymentSources(@NotNull Project project) {
    if (!PlatformUtils.isIdeaCommunity()) {
      return ImmutableList.of();
    }

    GradleProjectService projectService = GradleProjectService.getInstance();
    List<DeploymentSource> moduleDeploymentSources = Lists.newArrayList();

    Stream.of(ModuleManager.getInstance(project).getModules())
        .forEach(
            module -> {
              if (projectService.isGradleModule(module)
                  && AppEngineStandardFacet.hasFacet(module)
                  && AppEngineStandardGradleModuleComponent.getInstance(module)
                      .getGradleBuildDir()
                      .isPresent()) {
                moduleDeploymentSources.add(
                    new GradlePluginDeploymentSource(
                        ModulePointerManager.getInstance(project).create(module)));
              }
            });

    return moduleDeploymentSources;
  }
}
