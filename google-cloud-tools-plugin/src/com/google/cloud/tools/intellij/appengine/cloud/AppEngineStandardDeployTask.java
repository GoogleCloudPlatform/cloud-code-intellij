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

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Runnable that executes task responsible for deploying an application to the App Engine standard
 * environment.
 */
public class AppEngineStandardDeployTask extends AppEngineTask {

  private static final Logger logger = Logger.getInstance(AppEngineStandardDeployTask.class);

  private AppEngineDeploy deploy;
  private AppEngineStandardStage stageStandard;
  private boolean isFlexCompat;

  /**
   * @param isFlexCompat does not change any behavior of actual deployment. Provided solely for the
   *                     purpose of Analytics usage reporting.
   */
  public AppEngineStandardDeployTask(
      @NotNull AppEngineDeploy deploy,
      @NotNull AppEngineStandardStage stageStandard,
      boolean isFlexCompat) {
    this.deploy = deploy;
    this.stageStandard = stageStandard;
    this.isFlexCompat = isFlexCompat;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_DEPLOY)
        .withLabel(isFlexCompat ? "flex-compat" : "standard")
        .ping();

    File stagingDirectory;
    AppEngineHelper helper = deploy.getHelper();

    try {
      stagingDirectory = helper.createStagingDirectory(
          deploy.getLoggingHandler(),
          deploy.getDeploymentConfiguration().getCloudProjectName());
    } catch (IOException ioe) {
      deploy.getCallback().errorOccurred(
          GctBundle.message("appengine.deployment.error.creating.staging.directory"));
      logger.warn(ioe);
      return;
    }

    try {
      if (helper.stageCredentials(
          deploy.getDeploymentConfiguration().getGoogleUsername()) == null) {
        deploy.getCallback().errorOccurred(
            GctBundle.message("appengine.staging.credentials.error.message"));
        return;
      }

      stageStandard.stage(
          stagingDirectory,
          startListener,
          deploy(stagingDirectory, startListener));
    } catch (RuntimeException re) {
      deploy.getCallback()
          .errorOccurred(GctBundle.message("appengine.deployment.error.during.staging") + "\n"
              + GctBundle.message("appengine.action.error.update.message"));
      logger.error(re);
    }
  }

  @VisibleForTesting
  ProcessExitListener deploy(
      @NotNull final File stagingDirectory,
      @NotNull final ProcessStartListener startListener) {
    return new ProcessExitListener() {
      @Override
      public void onExit(int exitCode) {
        if (exitCode == 0) {
          try {
            deploy.deploy(stagingDirectory, startListener);
          } catch (RuntimeException re) {
            deploy.getCallback()
                .errorOccurred(GctBundle.message("appengine.deployment.error") + "\n"
                    + GctBundle.message("appengine.action.error.update.message"));
            logger.error(re);
          }
        } else {
          deploy.getCallback()
              .errorOccurred(GctBundle.message("appengine.deployment.error.during.staging"));
          logger.warn(
              "App engine standard staging process exited with an error. Exit Code:" + exitCode);
        }
      }
    };
  }
}
