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

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineStop;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.diagnostic.Logger;

/** Runnable that executes task responsible for stopping an App Engine application. */
public class AppEngineStopTask extends AppEngineTask {
  private static final Logger logger = Logger.getInstance(AppEngineStopTask.class);

  private AppEngineStop stop;
  private String module;
  private String version;

  /** Initialize the stop runner's dependencies. */
  public AppEngineStopTask(AppEngineStop stop, String module, String version) {
    this.stop = stop;
    this.module = module;
    this.version = version;
  }

  @Override
  public void execute(ProcessStartListener startListener) {
    UsageTrackerProvider.getInstance().trackEvent(GctTracking.APP_ENGINE_STOP).ping();

    try {
      if (stop.getHelper().stageCredentials(stop.getDeploymentConfiguration().getGoogleUsername())
          == null) {
        stop.getCallback()
            .errorOccurred(GctBundle.message("appengine.staging.credentials.error.message"));
        return;
      }

      stop.stop(module, version, startListener);
    } catch (RuntimeException re) {
      stop.getCallback().errorOccurred(GctBundle.message("appengine.stop.modules.version.error"));
      logger.error(re);
    }
  }
}
