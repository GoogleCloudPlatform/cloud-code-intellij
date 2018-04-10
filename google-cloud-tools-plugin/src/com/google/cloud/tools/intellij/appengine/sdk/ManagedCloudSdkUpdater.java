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

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.Notification;
import com.intellij.openapi.components.ServiceManager;
import java.awt.event.ActionListener;
import java.time.Clock;
import java.util.Timer;
import java.util.TimerTask;

/** Schedules and manages automatic updates for {@link ManagedCloudSdkService}. */
public class ManagedCloudSdkUpdater {
  private static ManagedCloudSdkUpdater instance;

  // one week.
  @VisibleForTesting static final long SDK_UPDATE_INTERVAL = 1000 * 60 * 60 * 24 * 7;
  // 20 seconds to show notification and then proceed with update.
  private static final int UPDATE_NOTIFICATION_DELAY = 1000 * 20;

  @VisibleForTesting
  static final String SDK_UPDATER_THREAD_NAME = "managed-google-cloud-sdk-updates";

  static ManagedCloudSdkUpdater getInstance() {
    if (instance == null) {
      instance = new ManagedCloudSdkUpdater();
    }
    return instance;
  }

  @VisibleForTesting
  static void setInstance(ManagedCloudSdkUpdater instance) {
    ManagedCloudSdkUpdater.instance = instance;
  }

  private Timer updateTimer;
  private TimerTask sdkUpdateTask;

  void activate() {
    if (!CloudSdkServiceUserSettings.getInstance().getEnableAutomaticUpdates()) {
      return;
    }

    if (updateTimer == null) {
      updateTimer = new Timer(SDK_UPDATER_THREAD_NAME);
    }
    // cancel tasks from previous activation.
    if (sdkUpdateTask != null) {
      sdkUpdateTask.cancel();
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
    long delayBeforeCheckMillis = SDK_UPDATE_INTERVAL;
    long now = getClock().millis();
    if (lastTimeOfUpdate != 0 && lastTimeOfUpdate < now) {
      delayBeforeCheckMillis =
          Math.min(
              SDK_UPDATE_INTERVAL,
              Math.max(0, SDK_UPDATE_INTERVAL - (getClock().millis() - lastTimeOfUpdate)));
    }

    schedule(sdkUpdateTask, delayBeforeCheckMillis, SDK_UPDATE_INTERVAL);
  }

  @VisibleForTesting
  Clock getClock() {
    return Clock.systemDefaultZone();
  }

  @VisibleForTesting
  void schedule(TimerTask timerTask, long delay, long period) {
    updateTimer.scheduleAtFixedRate(timerTask, delay, period);
  }

  @VisibleForTesting
  javax.swing.Timer createUiTimer(int delayMillis) {
    return new javax.swing.Timer(delayMillis, null);
  }

  private void doUpdate() {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();
    if (cloudSdkService instanceof ManagedCloudSdkService) {
      // do not show notifications and start update process if SDK is still up-to-date.
      boolean updateFeatureEnabled =
          ServiceManager.getService(PluginInfoService.class)
              .shouldEnable(GctFeature.MANAGED_SDK_UPDATE);

      if (updateFeatureEnabled && !((ManagedCloudSdkService) cloudSdkService).isUpToDate()) {
        // schedule UI timer to let a user decide about proceeding with the update.
        javax.swing.Timer uiTimer = createUiTimer(UPDATE_NOTIFICATION_DELAY);
        ActionListener cancelListener = (e) -> uiTimer.stop();
        ActionListener disableUpdateListener =
            (e) -> {
              uiTimer.stop();
              sdkUpdateTask.cancel();
              CloudSdkServiceUserSettings.getInstance().setEnableAutomaticUpdates(false);
            };
        Notification updateNotification =
            ManagedCloudSdkServiceUiPresenter.getInstance()
                .notifyManagedSdkUpdate(cancelListener, disableUpdateListener);

        uiTimer.addActionListener(
            e -> {
              updateNotification.expire();
              // grab a current SDK service, might have changed while waiting for notification.
              CloudSdkService afterNotificationCloudSdkService = CloudSdkService.getInstance();
              if (afterNotificationCloudSdkService instanceof ManagedCloudSdkService) {
                ((ManagedCloudSdkService) afterNotificationCloudSdkService).update();
              }
              uiTimer.stop();
            });

        uiTimer.start();
      }
    } else {
      // current SDK service is not managed SDk anymore, cancel the update until activated again.
      sdkUpdateTask.cancel();
    }
  }
}
