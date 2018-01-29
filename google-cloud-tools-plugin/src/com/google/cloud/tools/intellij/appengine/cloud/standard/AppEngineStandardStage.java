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

package com.google.cloud.tools.intellij.appengine.cloud.standard;

import com.google.cloud.tools.appengine.api.deploy.DefaultStageStandardConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineStandardStaging;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/** Stages an application in preparation for deployment to the App Engine flexible environment. */
public class AppEngineStandardStage {
  private CloudSdkAppEngineHelper helper;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private LoggingHandler loggingHandler;
  private Path deploymentArtifactPath;

  /** Initialize the staging dependencies. */
  public AppEngineStandardStage(
      @NotNull CloudSdkAppEngineHelper helper,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Path deploymentArtifactPath) {
    this.helper = helper;
    this.deploymentConfiguration = deploymentConfiguration;
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
      @NotNull Path stagingDirectory,
      @NotNull ProcessStartListener startListener,
      @NotNull ProcessExitListener onStageComplete) {

    ProcessOutputLineListener outputListener =
        new ProcessOutputLineListener() {
          @Override
          public void onOutputLine(String line) {
            loggingHandler.print(line + "\n");
          }
        };

    CloudSdk sdk =
        helper.createSdk(
            loggingHandler, startListener, outputListener, outputListener, onStageComplete);

    // TODO determine the default set of flags we want to set for AE standard staging
    DefaultStageStandardConfiguration stageConfig = new DefaultStageStandardConfiguration();
    stageConfig.setEnableJarSplitting(true);
    // TODO(joaomartins): Change File to Path on library configs.
    stageConfig.setStagingDirectory(stagingDirectory.toFile());
    stageConfig.setSourceDirectory(deploymentArtifactPath.toFile());

    // Other option is to move the DefaultStageStandardConfiguration into
    // AppEngineDeploymentConfiguration
    // TODO: add studd here
    stageConfig.setCompileEncoding(deploymentConfiguration.getCompileEncoding());
    stageConfig.setDeleteJsps(deploymentConfiguration.getDeleteJsps());
    stageConfig.setDisableJarJsps(deploymentConfiguration.getDisableJarJsps());
    stageConfig.setDisableUpdateCheck(deploymentConfiguration.getDisableUpdateCheck());
    stageConfig.setEnableJarClasses(deploymentConfiguration.getEnableJarClasses());
    stageConfig.setEnableJarSplitting(deploymentConfiguration.getEnableJarSplitting());
    stageConfig.setEnableQuickstart(deploymentConfiguration.getEnableQuickstart());
    stageConfig.setJarSplittingExcludes(deploymentConfiguration.getJarSplittingExcludes());

    CloudSdkAppEngineStandardStaging staging = new CloudSdkAppEngineStandardStaging(sdk);
    staging.stageStandard(stageConfig);
  }
}
