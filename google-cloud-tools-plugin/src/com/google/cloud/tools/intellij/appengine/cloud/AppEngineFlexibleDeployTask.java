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

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runnable that executes a task responsible for deploying an application to the App Engine
 * flexible environment.
 */
public class AppEngineFlexibleDeployTask extends AppEngineTask {
  private static final Logger logger = Logger.getInstance(AppEngineFlexibleDeployTask.class);

  private AppEngineDeploy deploy;
  private AppEngineFlexibleStage flexibleStage;

  public AppEngineFlexibleDeployTask(
      AppEngineDeploy deploy,
      AppEngineFlexibleStage flexibleStage) {
    this.deploy = deploy;
    this.flexibleStage = flexibleStage;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_DEPLOY)
        .withLabel("flex." + (deploy.getDeploymentConfiguration().isAuto() ? "auto" : "custom"))
        .ping();

    Path stagingDirectory;
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
      flexibleStage.stage(stagingDirectory);
    } catch (RuntimeException re) {
      deploy.getCallback()
          .errorOccurred(GctBundle.message("appengine.deployment.error.during.staging"));
      logger.error(re);
      return;
    }

    try {
      if (helper.stageCredentials(
          deploy.getDeploymentConfiguration().getGoogleUsername()) == null) {
        deploy.getCallback().errorOccurred(
            GctBundle.message("appengine.staging.credentials.error.message"));
        return;
      }

      deploy.deploy(stagingDirectory, startListener);
    } catch (RuntimeException re) {
      deploy.getCallback().errorOccurred(GctBundle.message("appengine.deployment.error") + "\n"
          + GctBundle.message("appengine.action.error.update.message"));
      logger.error(re);
    }
  }

  @Override
  void onCancel() {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_DEPLOY_CANCEL)
        .withLabel("flex")
        .ping();
  }
}
