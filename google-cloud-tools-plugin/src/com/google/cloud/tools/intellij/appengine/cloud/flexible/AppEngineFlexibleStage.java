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

import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
      AppEngineFlexibleFacet flexibleFacet =
          AppEngineFlexibleFacet.getFacetByModuleName(
              deploymentConfiguration.getModuleName(), project);

      if (flexibleFacet == null
          || !Files.exists(Paths.get(flexibleFacet.getConfiguration().getAppYamlPath()))) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.yaml"));
      }

      // Checks if the Yaml or Dockerfile exist before staging.
      // This should only happen in special circumstances, since the deployment UI prevents the
      // run config from being ran is the specified configuration files don't exist.
      AppEngineFlexibleFacetConfiguration facetConfiguration = flexibleFacet.getConfiguration();
      boolean isCustomRuntime =
          AppEngineProjectService.getInstance().getFlexibleRuntimeFromAppYaml(
              facetConfiguration.getAppYamlPath())
          .filter(runtime -> runtime == FlexibleRuntime.CUSTOM)
          .isPresent();

      if (isCustomRuntime
          && (!Files.isRegularFile(
          Paths.get(facetConfiguration.getDockerDirectory(), DOCKERFILE_NAME)))) {
        throw new RuntimeException(
            GctBundle.getString("appengine.deployment.error.staging.dockerfile"));
      }

      Path stagedArtifactPath = stagingDirectory.resolve(
          "target" + AppEngineFlexibleDeploymentArtifactType.typeForPath(deploymentArtifactPath));
      Files.copy(deploymentArtifactPath, stagedArtifactPath);

      Path appYamlPath = Paths.get(facetConfiguration.getAppYamlPath());
      Files.copy(appYamlPath, stagingDirectory.resolve(appYamlPath.getFileName()));

      if (isCustomRuntime) {
        File dockerDirectory = Paths.get(facetConfiguration.getDockerDirectory()).toFile();
        FileUtils.copyDirectory(
            dockerDirectory,
            stagingDirectory.toFile());
      }
    } catch (IOException | InvalidPathException | MalformedYamlFileException ex) {
      loggingHandler.print(ex.getMessage() + "\n");
      throw new RuntimeException(ex);
    }
  }
}
