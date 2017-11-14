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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.login.util.TrackerMessageBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;

/**
 * Creates notification to allow user to opt in/out of usage tracking for Cloud Tools plugin.
 */
public class UsageTrackerNotification {

  private static final Logger LOG = Logger.getInstance(UsageTrackerNotification.class);
  private static final UsageTrackerNotification INSTANCE = new UsageTrackerNotification();
  private final UsageTrackerManager usageTrackerManager;

  private UsageTrackerNotification() {
    usageTrackerManager = UsageTrackerManager.getInstance();
  }

  public static UsageTrackerNotification getInstance() {
    return INSTANCE;
  }

  /**
   * Show the notification panel.
   */
  public void showNotification() {
    NotificationListener listener = new NotificationListener() {
      @Override
      public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final String description = event.getDescription();
          if ("allow".equals(description)) {
            usageTrackerManager.setTrackingPreference(true);
            notification.expire();
          } else if ("decline".equals(description)) {
            UsageTrackerManager usageTrackerManager = UsageTrackerManager.getInstance();
            usageTrackerManager.setTrackingPreference(false);
            notification.expire();
          } else if ("policy".equals(description)) {
            try {
              BrowserUtil
                  .browse(new URL(UsageTrackerPanel.PRIVACY_POLICY_URL));
            } catch (MalformedURLException ex) {
              LOG.error(ex);
            }
            notification.expire();
          } else if ("settings".equals(description)) {
            final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
            util.showSettingsDialog(null, UsageTrackerConfigurable.class);
            notification.expire();
          }
        }

      }
    };

    Notification notification = new Notification(
        TrackerMessageBundle.message("notification.group.display.id"),
        TrackerMessageBundle.message("notification.popup.title"),
        TrackerMessageBundle.message("notification.popup.content"),
        NotificationType.INFORMATION, listener);
    Notifications.Bus.notify(notification);
  }
}
