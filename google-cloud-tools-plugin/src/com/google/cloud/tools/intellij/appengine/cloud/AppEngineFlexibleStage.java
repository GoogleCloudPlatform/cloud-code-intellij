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

import com.google.cloud.tools.appengine.api.deploy.DefaultStageFlexibleConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineFlexibleStaging;
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;

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
    File appYamlPath = deploymentConfiguration.isCustom()
        ? CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getAppYamlPath())
        : helper.defaultAppYaml();

    DefaultStageFlexibleConfiguration stageConfig = new DefaultStageFlexibleConfiguration();
    stageConfig.setStagingDirectory(stagingDirectory);
    stageConfig.setArtifact(deploymentArtifactPath);
    stageConfig.setAppYaml(appYamlPath);
    if (deploymentConfiguration.isCustom()) {
      File customDockerfile
          = CloudSdkUtil.getFileFromFilePath(deploymentConfiguration.getDockerFilePath());
      stageConfig.setDockerfile(customDockerfile);
    }

    CloudSdkAppEngineFlexibleStaging staging = new CloudSdkAppEngineFlexibleStaging();
    staging.stageFlexible(stageConfig);
  }
}
