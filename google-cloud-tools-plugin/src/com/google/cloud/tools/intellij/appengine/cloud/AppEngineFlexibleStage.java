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

import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Stages an application in preparation for deployment to the App Engine flexible environment.
 */
public class AppEngineFlexibleStage {

  private static final Logger logger = Logger.getInstance(AppEngineFlexibleStage.class);

  private CloudSdkAppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private File deploymentArtifactPath;
  private AppEngineDeploymentConfiguration deploymentConfiguration;

  /**
   * Initialize the staging dependencies.
   */
  public AppEngineFlexibleStage(
      @NotNull CloudSdkAppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull File deploymentArtifactPath,
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
  public void stage(@NotNull File stagingDirectory) {
    try {
      File stagedArtifactPath =
          copyFile(
              stagingDirectory,
              "target" + AppEngineFlexDeploymentArtifactType.typeForPath(deploymentArtifactPath),
              deploymentArtifactPath);
      stagedArtifactPath.setReadable(true /* readable */, false /* ownerOnly */);

      File appYamlPath = deploymentConfiguration.isAuto()
          ? helper.defaultAppYaml()
          : CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getAppYamlPath());

      File dockerFilePath = deploymentConfiguration.isAuto()
          ? helper.defaultDockerfile(
          AppEngineFlexDeploymentArtifactType.typeForPath(deploymentArtifactPath))
          : CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getDockerFilePath());

      copyFile(stagingDirectory, "app.yaml", appYamlPath);
      copyFile(stagingDirectory, "Dockerfile", dockerFilePath);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private File copyFile(File stagingDirectory, String targetFileName, File sourceFilePath)
      throws IOException {
    File destinationFilePath = new File(stagingDirectory, targetFileName);
    FileUtil.copy(sourceFilePath, destinationFilePath);
    loggingHandler.print(String.format("Copied %s %s to %s", targetFileName,
        sourceFilePath.getAbsolutePath(), destinationFilePath.getAbsolutePath()) +  "\n");
    return destinationFilePath;
  }
}
