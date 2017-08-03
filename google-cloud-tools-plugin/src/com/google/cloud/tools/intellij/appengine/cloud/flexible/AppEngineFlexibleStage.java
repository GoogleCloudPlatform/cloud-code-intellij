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

/** Stages an application in preparation for deployment to the App Engine flexible environment. */
public class AppEngineFlexibleStage {

  private static final String DOCKERFILE_NAME = "Dockerfile";

  private final LoggingHandler loggingHandler;
  private final Path deploymentArtifactPath;
  private final AppEngineDeploymentConfiguration deploymentConfiguration;
  private final Project project;

  /** Initialize the staging dependencies. */
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
   * Stages the application in the given staging directory, in preparation for deployment to the App
   * Engine flexible environment.
   *
   * @param stagingDirectory the directory to stage the application to
   * @return {@code true} if the application was staged successfully, {@code false} otherwise
   * @throws IOException if there was a filesystem error during copying
   */
  public boolean stage(@NotNull Path stagingDirectory) throws IOException {
    try {
      String moduleName = deploymentConfiguration.getModuleName();
      if (StringUtils.isEmpty(moduleName)) {
        loggingHandler.print(getMessage("appengine.deployment.error.staging.yaml.notspecified"));
        return false;
      }

      AppEngineFlexibleFacet flexibleFacet =
          AppEngineFlexibleFacet.getFacetByModuleName(moduleName, project);
      if (flexibleFacet == null
          || StringUtils.isEmpty(flexibleFacet.getConfiguration().getAppYamlPath())) {
        loggingHandler.print(getMessage("appengine.deployment.error.staging.yaml.notspecified"));
        return false;
      }

      AppEngineFlexibleFacetConfiguration facetConfiguration = flexibleFacet.getConfiguration();
      String appYaml = facetConfiguration.getAppYamlPath();

      // Checks if the app.yaml exists before staging.
      if (!Files.exists(Paths.get(appYaml))) {
        loggingHandler.print(getMessage("appengine.deployment.error.staging.yaml.notfound"));
        return false;
      }

      boolean isCustomRuntime =
          AppEngineProjectService.getInstance()
              .getFlexibleRuntimeFromAppYaml(appYaml)
              .filter(FlexibleRuntime::isCustom)
              .isPresent();

      // Checks if the Dockerfile exists before staging.
      String dockerDirectory = facetConfiguration.getDockerDirectory();
      if (isCustomRuntime) {
        if (Strings.isNullOrEmpty(dockerDirectory)) {
          loggingHandler.print(
              getMessage("appengine.deployment.error.staging.dockerfile.notspecified"));
          return false;
        }
        if (!Files.isRegularFile(Paths.get(dockerDirectory, DOCKERFILE_NAME))) {
          loggingHandler.print(
              getMessage("appengine.deployment.error.staging.dockerfile.notfound"));
          return false;
        }
      }

      Path stagedArtifactPath =
          stagingDirectory.resolve(
              "target"
                  + AppEngineFlexibleDeploymentArtifactType.typeForPath(deploymentArtifactPath));
      Files.copy(deploymentArtifactPath, stagedArtifactPath);

      Path appYamlPath = Paths.get(appYaml);
      Files.copy(appYamlPath, stagingDirectory.resolve(appYamlPath.getFileName()));

      if (isCustomRuntime) {
        FileUtils.copyDirectory(Paths.get(dockerDirectory).toFile(), stagingDirectory.toFile());
      }
    } catch (InvalidPathException | MalformedYamlFileException e) {
      loggingHandler.print(e.getMessage() + "\n");
    }
    return true;
  }

  /**
   * Returns the message associated with the given key, as described by {@link GctBundle}, appended
   * by a newline.
   *
   * @param messageKey the key of the message (as described by {@link GctBundle#message}) to show in
   *     the error
   */
  private static String getMessage(String messageKey) {
    return GctBundle.message(messageKey) + "\n";
  }
}
