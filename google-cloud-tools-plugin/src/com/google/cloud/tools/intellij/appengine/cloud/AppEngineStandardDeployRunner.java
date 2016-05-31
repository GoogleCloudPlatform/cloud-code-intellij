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

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploy.AppEngineDeployException;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineStandardStage.AppEngineStandardStageException;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Runnable that executes task responsible for deploying an application to the App Engine
 * standard environment.
 */
public class AppEngineStandardDeployRunner implements Runnable {
  private static final Logger logger = Logger.getInstance(AppEngineStandardDeployRunner.class);

  private AppEngineDeploy deploy;
  private AppEngineStandardStage stageStandard;

  public AppEngineStandardDeployRunner(
      @NotNull AppEngineDeploy deploy,
      @NotNull AppEngineStandardStage stageStandard) {
    this.deploy = deploy;
    this.stageStandard = stageStandard;
  }

  @Override
  public void run() {
    try {
      final File stagingDirectory =
          deploy.getHelper().createStagingDirectory(deploy.getLoggingHandler());

      deploy.getHelper().stageCredentials(deploy.getDeploymentConfiguration().getGoogleUsername());

      stageStandard.stage(stagingDirectory, new ProcessExitListener() {
        @Override
        public void exit(int exitCode) {
          try {
            // TODO figure out cancel
            deploy.deploy(stagingDirectory);
          } catch (AppEngineDeployException de) {
            logger.warn(de.getMessage());
            deploy.getCallback().errorOccurred(de.getMessage());
          }
        }
      });
    } catch (AppEngineStandardStageException | IOException ex) {
      logger.warn(ex.getMessage());
      deploy.getCallback().errorOccurred(ex.getMessage());
    }
  }
}
