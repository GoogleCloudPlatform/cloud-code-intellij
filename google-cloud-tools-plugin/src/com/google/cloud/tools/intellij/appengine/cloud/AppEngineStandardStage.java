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

import com.google.cloud.tools.app.impl.cloudsdk.CloudSdkAppEngineStandardStaging;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessOutputLineListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.sdk.CloudSdk;
import com.google.cloud.tools.app.impl.config.DefaultStageStandardConfiguration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Stages an application in preparation for deployment to the App Engine flexible environment.
 */
public class AppEngineStandardStage {

  private static final Logger logger = Logger.getInstance(AppEngineStandardStage.class);

  private CloudSdkAppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private File deploymentArtifactPath;


  /**
   * Initialize the staging dependencies.
   */
  public AppEngineStandardStage(
      @NotNull CloudSdkAppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull File deploymentArtifactPath) {
    this.helper = helper;
    this.loggingHandler = loggingHandler;
    this.deploymentArtifactPath = deploymentArtifactPath;
  }

  /**
   * Stage the application in preparation for deployment to the App Engine standard environment.
   *
   * @param stagingDirectory the local staging directory
   * @param onStageComplete a callback for executing actions on completion of staging
   */
  public void stage(
      @NotNull File stagingDirectory,
      @NotNull ProcessExitListener onStageComplete) {

    ProcessOutputLineListener outputListener = new ProcessOutputLineListener() {
      @Override
      public void outputLine(String line) {
        loggingHandler.print(line + "\n");
      }
    };

    CloudSdk sdk = helper.createSdk(
        loggingHandler,
        outputListener,
        outputListener,
        onStageComplete);

    // TODO determine the default set of flags we want to set for AE standard staging
    DefaultStageStandardConfiguration stageConfig = new DefaultStageStandardConfiguration();
    stageConfig.setEnableJarSplitting(true);
    stageConfig.setStagingDirectory(stagingDirectory);
    stageConfig.setSourceDirectory(deploymentArtifactPath);

    CloudSdkAppEngineStandardStaging staging = new CloudSdkAppEngineStandardStaging(sdk);
    staging.stageStandard(stageConfig);
  }
}
