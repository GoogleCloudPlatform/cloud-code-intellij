/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetConfiguration;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Stages an application in preparation for deployment to the App Engine flexible environment.
 */
public class AppEngineFlexibleStage {
  private static final String DOCKERFILE_NAME = "Dockerfile";

  private LoggingHandler loggingHandler;
  private Path deploymentArtifactPath;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private Project project;

  /**
   * Initialize the staging dependencies.
   */
  public AppEngineFlexibleStage(
      @NotNull LoggingHandler loggingHandler,
      @NotNull Path deploymentArtifactPath,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull Project project) {
    this.loggingHandler = loggingHandler;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.deploymentConfiguration = deploymentConfiguration;
    this.project = project;
  }

  /**
   * Given a local staging directory, stage the application in preparation for deployment to the
   * App Engine flexible environment.
   */
  public void stage(@NotNull Path stagingDirectory) {
    try {
      String moduleName = deploymentConfiguration.getModuleName();
      if (StringUtils.isEmpty(moduleName)) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.yaml.notspecified"));
      }

      AppEngineFlexibleFacet flexibleFacet =
          AppEngineFlexibleFacet.getFacetByModuleName(moduleName, project);
      if (flexibleFacet == null
          || Strings.isNullOrEmpty(flexibleFacet.getConfiguration().getAppYamlPath())) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.yaml.notspecified"));
      }

      AppEngineFlexibleFacetConfiguration facetConfiguration = flexibleFacet.getConfiguration();
      String appYaml = facetConfiguration.getAppYamlPath();

      // Checks if the app.yaml exists before staging.
      if (!Files.exists(Paths.get(appYaml))) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.yaml.notfound"));
      }

      boolean isCustomRuntime =
          AppEngineProjectService.getInstance()
              .getFlexibleRuntimeFromAppYaml(appYaml)
              .filter(runtime -> runtime == FlexibleRuntime.CUSTOM)
              .isPresent();

      // Checks if the Dockerfile exists before staging.
      Path dockerDirectoryPath = Paths.get(facetConfiguration.getDockerDirectory());
      if (isCustomRuntime && !Files.isRegularFile(dockerDirectoryPath.resolve(DOCKERFILE_NAME))) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
      }

      Path stagedArtifactPath =
          stagingDirectory.resolve(
              "target"
                  + AppEngineFlexibleDeploymentArtifactType.typeForPath(deploymentArtifactPath));
      Files.copy(deploymentArtifactPath, stagedArtifactPath);

      Path appYamlPath = Paths.get(appYaml);
      Files.copy(appYamlPath, stagingDirectory.resolve(appYamlPath.getFileName()));

      if (isCustomRuntime) {
        FileUtils.copyDirectory(dockerDirectoryPath.toFile(), stagingDirectory.toFile());
      }
    } catch (IOException | InvalidPathException | MalformedYamlFileException ex) {
      loggingHandler.print(ex.getMessage() + "\n");
      throw new RuntimeException(ex);
    }
  }
}
