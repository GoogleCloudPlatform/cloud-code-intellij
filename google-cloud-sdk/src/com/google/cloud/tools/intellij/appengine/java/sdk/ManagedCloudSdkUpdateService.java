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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.Notification;
import com.intellij.openapi.components.ServiceManager;
import java.awt.event.ActionListener;
import java.time.Clock;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/** Schedules and manages automatic updates for {@link ManagedCloudSdkService}. */
public class ManagedCloudSdkUpdateService {
  // one week.
  @VisibleForTesting static final long SDK_UPDATE_INTERVAL_MS = 1000 * 60 * 60 * 24 * 7;
  // 20 seconds to show notification and then proceed with update.
  private static final int UPDATE_NOTIFICATION_EXPIRATION_TIME_MS = 1000 * 20;

  @VisibleForTesting
  static final String SDK_UPDATER_THREAD_NAME = "managed-google-cloud-sdk-updates";

  static ManagedCloudSdkUpdateService getInstance() {
    return ServiceManager.getService(ManagedCloudSdkUpdateService.class);
  }

  private Timer updateTimer;
  private TimerTask sdkUpdateTask;

  void activate() {
    if (!CloudSdkServiceUserSettings.getInstance().isAutomaticUpdateEnabled()) {
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

    schedule(sdkUpdateTask, getDelayBeforeFirstUpdate(), SDK_UPDATE_INTERVAL_MS);
  }

  /**
   * Called when managed SDK update operation (update or install) completes, either with success or
   * failure.
   */
  void notifySdkUpdateCompleted() {
    CloudSdkServiceUserSettings.getInstance().setLastAutomaticUpdateTimestamp(getClock().millis());
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

  /**
   * Calculates milliseconds delay before first update based on last update/install time. If
   * complete update interval or more elapsed, first update is scheduled immediately (returns 0).
   */
  private long getDelayBeforeFirstUpdate() {
    Optional<Long> lastTimeOfUpdate =
        CloudSdkServiceUserSettings.getInstance().getLastAutomaticUpdateTimestamp();
    long delayBeforeUpdateMillis = SDK_UPDATE_INTERVAL_MS;
    if (lastTimeOfUpdate.isPresent()) {
      delayBeforeUpdateMillis =
          Math.min(
              SDK_UPDATE_INTERVAL_MS,
              Math.max(0, SDK_UPDATE_INTERVAL_MS - (getClock().millis() - lastTimeOfUpdate.get())));
    }

    return delayBeforeUpdateMillis;
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
        javax.swing.Timer uiTimer = createUiTimer(UPDATE_NOTIFICATION_EXPIRATION_TIME_MS);
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
      // current SDK service is not managed SDK anymore, cancel the update until activated again.
      sdkUpdateTask.cancel();
    }
  }
}
