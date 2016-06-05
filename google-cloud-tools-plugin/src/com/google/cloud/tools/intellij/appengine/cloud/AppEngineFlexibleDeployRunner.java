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

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.impl.CancellableRunnable;

import java.io.File;
import java.io.IOException;

/**
 * Runnable that executes task responsible for deploying an application to the App Engine
 * flexible environment.
 */
public class AppEngineFlexibleDeployRunner implements CancellableRunnable {
  private static final Logger logger = Logger.getInstance(AppEngineFlexibleDeployRunner.class);

  private AppEngineDeploy deploy;
  private AppEngineFlexibleStage flexibleStage;

  private Process process;

  public AppEngineFlexibleDeployRunner(
      AppEngineDeploy deploy,
      AppEngineFlexibleStage flexibleStage) {
    this.deploy = deploy;
    this.flexibleStage = flexibleStage;
  }

  @Override
  public void run() {
    File stagingDirectory;

    try {
      stagingDirectory = deploy.getHelper().createStagingDirectory(deploy.getLoggingHandler());
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
      deploy.getHelper().stageCredentials(deploy.getDeploymentConfiguration().getGoogleUsername());

      deploy.deploy(stagingDirectory, new ProcessStartListener() {
        @Override
        public void start(Process process) {
          setProcess(process);
        }
      });
    } catch (RuntimeException re) {
      deploy.getCallback().errorOccurred(GctBundle.message("appengine.deployment.error") + "\n"
          + GctBundle.message("appengine.action.error.update.message"));
      logger.error(re);
    }
  }

  @Override
  public void cancel() {
    if (process != null) {
      process.destroy();
    }
  }

  private void setProcess(Process process) {
    this.process = process;
  }
}
