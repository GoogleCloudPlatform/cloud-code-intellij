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

package com.google.cloud.tools.intellij.startup;

import com.google.cloud.tools.intellij.analytics.UsageTrackerNotification;
import com.google.cloud.tools.intellij.analytics.UsageTrackingManagementService;
import com.google.cloud.tools.intellij.appengine.java.startup.ConflictingAppEnginePluginCheck;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.util.TrackerMessageBundle;
import com.google.cloud.tools.intellij.service.PluginConfigurationService;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

/** Performs runtime initialization for the GCT plugin. */
public class CloudToolsPluginInitializationComponent implements ApplicationComponent {

  @Override
  public void disposeComponent() {
    // Do nothing.
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleCloudToolsCore.InitializationComponent";
  }

  @Override
  public void initComponent() {

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      configureUsageTracking();
    }
    Services.getLoginService().loadPersistedCredentials();
    PluginConfigurationService pluginConfigurationService =
        ServiceManager.getService(PluginConfigurationService.class);
    PluginInfoService pluginInfoService = ServiceManager.getService(PluginInfoService.class);

    if (pluginInfoService.shouldEnableErrorFeedbackReporting()) {
      initErrorReporting(pluginConfigurationService, pluginInfoService);
    }

    new ConflictingAppEnginePluginCheck().notifyIfConflicting();
    new GoogleAccountPluginUninstaller().uninstallIfPresent();
  }

  /**
   * For Google Usage Tracker Ensure that the notification manager (also an application component)
   * is registered first; otherwise this component's initComponent() call will fire a notification
   * event bus to show the opt-in dialog, but the notification component may not yet have been
   * initialized and is therefore not subscribed and listening.
   */
  @VisibleForTesting
  void configureUsageTracking() {
    UsageTrackingManagementService usageTrackingManagementService =
        UsageTrackingManagementService.getInstance();
    if (usageTrackingManagementService.isUsageTrackingAvailable()
        && !usageTrackingManagementService.hasUserRecordedTrackingPreference()) {
      NotificationsManager.getNotificationsManager();
      NotificationsConfiguration.getNotificationsConfiguration()
          .register(
              TrackerMessageBundle.message("notification.group.display.id"),
              NotificationDisplayType.STICKY_BALLOON);
      UsageTrackerNotification.getInstance().showNotification();

      UsageTrackingManagementService.getInstance().setTrackingPreference(true);
    }
  }

  private void initErrorReporting(
      PluginConfigurationService pluginConfigurationService, PluginInfoService pluginInfoService) {
    pluginConfigurationService.enabledGoogleFeedbackErrorReporting(pluginInfoService.getPluginId());
  }
}
