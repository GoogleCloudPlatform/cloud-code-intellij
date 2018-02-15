/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.executor;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploy;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardStage;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

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
   *     purpose of Analytics usage reporting.
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
        .addMetadata(GctTracking.METADATA_LABEL_KEY, isFlexCompat ? "flex-compat" : "standard")
        .ping();

    Path stagingDirectory;
    AppEngineHelper helper = deploy.getHelper();

    try {
      stagingDirectory =
          helper.createStagingDirectory(
              deploy.getLoggingHandler(),
              deploy.getDeploymentConfiguration().getCloudProjectName());
    } catch (IOException ioe) {
      deploy
          .getCallback()
          .errorOccurred(
              GctBundle.message("appengine.deployment.error.creating.staging.directory"));
      logger.error(ioe);
      return;
    }

    try {
      if (helper.stageCredentials(deploy.getDeploymentConfiguration().getGoogleUsername())
          == null) {
        deploy
            .getCallback()
            .errorOccurred(GctBundle.message("appengine.staging.credentials.error.message"));
        return;
      }

      stageStandard.stage(stagingDirectory, startListener, deploy(stagingDirectory, startListener));
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      deploy
          .getCallback()
          .errorOccurred(
              GctBundle.message("appengine.cloudsdk.java.components.missing")
                  + "\n"
                  + GctBundle.message("appengine.cloudsdk.java.components.howtoinstall"));
      logger.warn(ex);
    } catch (RuntimeException re) {
      deploy
          .getCallback()
          .errorOccurred(
              GctBundle.message("appengine.deployment.exception.during.staging")
                  + "\n"
                  + GctBundle.message("appengine.action.error.update.message"));
      logger.error(re);
    }
  }

  @VisibleForTesting
  ProcessExitListener deploy(
      @NotNull final Path stagingDirectory, @NotNull final ProcessStartListener startListener) {
    return (exitCode) -> {
      if (exitCode == 0) {
        try {
          deploy.deploy(stagingDirectory, startListener);
        } catch (RuntimeException re) {
          deploy
              .getCallback()
              .errorOccurred(
                  GctBundle.message("appengine.deployment.exception")
                      + "\n"
                      + GctBundle.message("appengine.action.error.update.message"));
          logger.error(re);
        }
      } else {
        deploy
            .getCallback()
            .errorOccurred(
                GctBundle.message("appengine.deployment.error.during.staging", exitCode));
        logger.warn(
            "App engine standard staging process exited with an error. Exit Code:" + exitCode);
      }
    };
  }

  @Override
  void onCancel() {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_DEPLOY_CANCEL)
        .addMetadata(GctTracking.METADATA_LABEL_KEY, isFlexCompat ? "flex-compat" : "standard")
        .ping();
  }
}
