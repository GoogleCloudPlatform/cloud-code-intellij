/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.Timer;
import java.util.TimerTask;

/** Schedules and manages automatic updates for {@link ManagedCloudSdkService}. */
public class ManagedCloudSdkUpdater {
  @VisibleForTesting static final long SDK_UPDATE_INTERVAL = 1000 * 60 * 60 * 24 * 7; // one week.

  @VisibleForTesting
  static final String SDK_UPDATER_THREAD_NAME = "managed-google-cloud-sdk-updates";

  private Timer updateTimer;
  private TimerTask sdkUpdateTask;

  void activate() {
    if (updateTimer == null) {
      updateTimer = new Timer(SDK_UPDATER_THREAD_NAME);
    }

    sdkUpdateTask =
        new TimerTask() {
          @Override
          public void run() {
            doUpdate();
          }
        };
    long lastTimeOfUpdate =
        CloudSdkServiceUserSettings.getInstance().getLastAutomaticUpdateTimestamp();
    long delayBeforeCheckSeconds = SDK_UPDATE_INTERVAL;
    long now = getClock().millis();
    if (lastTimeOfUpdate != 0 && lastTimeOfUpdate < now) {
      delayBeforeCheckSeconds =
          Math.min(SDK_UPDATE_INTERVAL, getClock().millis() - lastTimeOfUpdate);
    }

    updateTimer.scheduleAtFixedRate(sdkUpdateTask, delayBeforeCheckSeconds, SDK_UPDATE_INTERVAL);
  }

  @VisibleForTesting
  Clock getClock() {
    return Clock.systemDefaultZone();
  }

  private void doUpdate() {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();
    if (cloudSdkService instanceof ManagedCloudSdkService) {
      ((ManagedCloudSdkService) cloudSdkService).update();
    } else {
      // current SDK service is not managed SDk anymore, cancel the update until activated again.
      sdkUpdateTask.cancel();
    }
  }
}
