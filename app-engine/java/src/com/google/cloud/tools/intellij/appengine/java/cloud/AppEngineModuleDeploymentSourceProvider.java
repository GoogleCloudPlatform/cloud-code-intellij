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

import com.google.cloud.tools.intellij.appengine.java.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.common.collect.Lists;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** An {@link AppEngineDeploymentSourceProvider} that collects module-based deployment sources. */
public class AppEngineModuleDeploymentSourceProvider implements AppEngineDeploymentSourceProvider {

  /**
   * Collects a list of module deployment sources available for deployment to App Engine:
   *
   * <p>Maven based deployment sources are included for both flexible and standard projects if
   * applicable.
   *
   * <p>User browsable jar/war deployment sources are included only if there are no App Engine
   * standard modules.
   *
   * @return a list of {@link ModuleDeploymentSource}
   */
  @Override
  public List<DeploymentSource> getDeploymentSources(@NotNull Project project) {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    List<DeploymentSource> moduleDeploymentSources = Lists.newArrayList();

    boolean hasStandardModules = false;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      FacetManager facetManager = FacetManager.getInstance(module);
      if (facetManager.getFacetByType(AppEngineStandardFacetType.ID) != null
          || facetManager.getFacetByType(AppEngineFlexibleFacetType.ID) != null) {
        AppEngineEnvironment environment =
            projectService.getModuleAppEngineEnvironment(module).orElse(null);

        if (environment != null) {
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
    }

    if (!hasStandardModules) {
      moduleDeploymentSources.add(createUserSpecifiedPathDeploymentSource(project));
    }

    return moduleDeploymentSources;
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
