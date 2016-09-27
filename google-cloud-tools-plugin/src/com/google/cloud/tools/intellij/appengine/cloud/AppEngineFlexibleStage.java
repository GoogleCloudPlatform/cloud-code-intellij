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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.common.collect.ImmutableSet;

import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;

/**
 * Stages an application in preparation for deployment to the App Engine flexible environment.
 */
public class AppEngineFlexibleStage {
  private CloudSdkAppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private Path deploymentArtifactPath;
  private AppEngineDeploymentConfiguration deploymentConfiguration;

  /**
   * Initialize the staging dependencies.
   */
  public AppEngineFlexibleStage(
      @NotNull CloudSdkAppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Path deploymentArtifactPath,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration) {
    this.helper = helper;
    this.loggingHandler = loggingHandler;
    this.deploymentArtifactPath = deploymentArtifactPath;
    this.deploymentConfiguration = deploymentConfiguration;
  }

  /**
   * Given a local staging directory, stage the application in preparation for deployment to the
   * App Engine flexible environment.
   */
  public void stage(@NotNull Path stagingDirectory) {
    try {
      Path stagedArtifactPath = stagingDirectory.resolve(
          "target" + AppEngineFlexDeploymentArtifactType.typeForPath(deploymentArtifactPath));
      Files.copy(deploymentArtifactPath, stagedArtifactPath);

      Path appYamlPath = deploymentConfiguration.isAuto()
          ? helper.defaultAppYaml()
          : Paths.get(deploymentConfiguration.getAppYamlPath());
      Files.copy(appYamlPath, stagingDirectory.resolve("app.yaml"));

      Path dockerFilePath = deploymentConfiguration.isAuto()
          ? helper.defaultDockerfile(
          AppEngineFlexDeploymentArtifactType.typeForPath(deploymentArtifactPath))
          : Paths.get(deploymentConfiguration.getDockerFilePath());
      Files.copy(dockerFilePath, stagingDirectory.resolve("Dockerfile"));
    } catch (IOException ex) {
      loggingHandler.print(ex.getMessage() + "\n");
      throw new RuntimeException(ex);
    }
  }
}
