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

package com.google.cloud.tools.intellij.appengine.java.maven;

import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploymentSourceProvider;
import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.java.maven.project.MavenProjectService;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** An {@link AppEngineDeploymentSourceProvider} that collects maven deployment sources. */
public class AppEngineMavenDeploymentSourceProvider implements AppEngineDeploymentSourceProvider {

  /**
   * Collects Maven based deployment sources for both flexible and standard App Engine projects if
   * the module has Maven support.
   *
   * @return a list of maven-based {@link DeploymentSource}
   */
  @Override
  public List<DeploymentSource> getDeploymentSources(@NotNull Project project) {
    AppEngineProjectService appEngineProjectService = AppEngineProjectService.getInstance();
    MavenProjectService mavenProjectService = MavenProjectService.getInstance();

    List<DeploymentSource> mavenDeploymentSources = Lists.newArrayList();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      boolean hasStandardFacet = AppEngineStandardFacet.hasFacet(module);
      boolean hasFlexibleFacet = AppEngineFlexibleFacet.hasFacet(module);

      if (hasStandardFacet || hasFlexibleFacet) {
        AppEngineEnvironment environment =
            appEngineProjectService.getModuleAppEngineEnvironment(module).orElse(null);

        if (environment != null) {
          if (ModuleType.is(module, JavaModuleType.getModuleType())
              && mavenProjectService.isJarOrWarMavenBuild(module)) {
            mavenDeploymentSources.add(
                createMavenBuildDeploymentSource(project, module, environment));
          }
        }
      }
    }

    return mavenDeploymentSources;
  }

  private static MavenBuildDeploymentSource createMavenBuildDeploymentSource(
      @NotNull Project project, @NotNull Module module, @NotNull AppEngineEnvironment environment) {
    return new MavenBuildDeploymentSource(
        ModulePointerManager.getInstance(project).create(module), project, environment);
  }
}
