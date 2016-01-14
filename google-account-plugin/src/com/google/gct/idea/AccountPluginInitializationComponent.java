package com.google.gct.idea;

import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.feedback.FeedbackUtil;

import com.google.gct.idea.util.PlatformInfo;
import com.google.gct.stats.UsageTrackerManager;
import com.google.gct.stats.UsageTrackerNotification;
import com.google.gct.login.util.TrackerMessageBundle;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Performs runtime initialization for the Google Login plugin.
 */
public class AccountPluginInitializationComponent implements ApplicationComponent {

  private static final String PLUGIN_ID = "com.google.gct.login";

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleLogin.InitializationComponent";
  }

  @Override
  public void initComponent() {
    initComponent(PlatformUtils.getPlatformPrefix());
  }

  @VisibleForTesting
  void initComponent(String platformPrefix) {
    if (PlatformInfo.SUPPORTED_PLATFORMS.contains(platformPrefix)) {
      enableFeedbackUtil();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        enableUsageTracking();
      }
    }

  }

  @Override
  public void disposeComponent() {
    // no-op
  }

  @VisibleForTesting
  void enableFeedbackUtil() {
    FeedbackUtil.enableGoogleFeedbackErrorReporting(PLUGIN_ID);
  }

  /**
   * For Google Usage Tracker
   * Ensure that the notification manager (also an application component) is registered first;
   * otherwise this component's initComponent() call will fire a notification event bus
   * to show the opt-in dialog, but the notification component may not yet have been initialized
   * and is therefore not subscribed and listening.
   */
  @VisibleForTesting
  void enableUsageTracking() {
    UsageTrackerManager usageTrackerManager = UsageTrackerManager.getInstance();
    NotificationsManager.getNotificationsManager();
    NotificationsConfiguration.getNotificationsConfiguration().register(
        TrackerMessageBundle.message("notification.group.display.id"),
        NotificationDisplayType.STICKY_BALLOON);
    if (usageTrackerManager.isUsageTrackingAvailable() && !usageTrackerManager.hasUserRecordedTrackingPreference()) {
      UsageTrackerNotification.getInstance().showNotification();
    }

  }
}
