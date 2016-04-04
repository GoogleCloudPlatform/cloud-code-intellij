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

package com.google.cloud.tools.intellij;

import com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.login.util.TrackerMessageBundle;
import com.google.cloud.tools.intellij.stats.UsageTrackerManager;
import com.google.cloud.tools.intellij.stats.UsageTrackerNotification;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;

/**
 * Performs runtime initialization for the Google Login plugin.
 */
public class AccountPluginInitializationComponent implements ApplicationComponent {

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleLogin.InitializationComponent";
  }

  @Override
  public void initComponent() {
    AccountPluginInfoService pluginInfoService =
        ServiceManager.getService(AccountPluginInfoService.class);
    AccountPluginConfigurationService pluginConfigurationService = ServiceManager
        .getService(AccountPluginConfigurationService.class);
    if (pluginInfoService.shouldEnableErrorFeedbackReporting()) {
      pluginConfigurationService.enabledGoogleFeedbackErrorReporting(pluginInfoService.getPluginId());
    }
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      configureUsageTracking();
    }
  }

  @Override
  public void disposeComponent() {
    // no-op
  }


  /**
   * For Google Usage Tracker
   * Ensure that the notification manager (also an application component) is registered first;
   * otherwise this component's initComponent() call will fire a notification event bus
   * to show the opt-in dialog, but the notification component may not yet have been initialized
   * and is therefore not subscribed and listening.
   */
  @VisibleForTesting
  void configureUsageTracking() {
    UsageTrackerManager usageTrackerManager = UsageTrackerManager.getInstance();
    if (usageTrackerManager.isUsageTrackingAvailable()
        && !usageTrackerManager.hasUserRecordedTrackingPreference()) {
      NotificationsManager.getNotificationsManager();
      NotificationsConfiguration.getNotificationsConfiguration().register(
          TrackerMessageBundle.message("notification.group.display.id"),
          NotificationDisplayType.STICKY_BALLOON);
      UsageTrackerNotification.getInstance().showNotification();
    }
  }
}
