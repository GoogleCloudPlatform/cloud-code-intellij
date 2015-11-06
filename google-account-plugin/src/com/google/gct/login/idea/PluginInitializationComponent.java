package com.google.gct.login.idea;

import com.google.gct.idea.feedback.FeedbackUtil;

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
public class PluginInitializationComponent implements ApplicationComponent {

  private static final String PLUGIN_ID = "com.google.gct.login";
  private UsageTrackerManager usageTrackerManager;

  public PluginInitializationComponent() {
    usageTrackerManager = UsageTrackerManager.getInstance();
  }

  public PluginInitializationComponent(UsageTrackerManager trackerManager) {
    usageTrackerManager = trackerManager;
  }

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleLogin.InitializationComponent";
  }

  @Override
  public void initComponent() {
    if (!"AndroidStudio".equals(PlatformUtils.getPlatformPrefix())) {
      FeedbackUtil.enableGoogleFeedbackErrorReporting(PLUGIN_ID);
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }

    // For Google Usage Tracker
    // Ensure that the notification manager (also an application component) is registered first;
    // otherwise this component's initComponent() call will fire a notification event bus
    // to show the opt-in dialog, but the notification component may not yet have been initialized
    // and is therefore not subscribed and listening.
    NotificationsManager.getNotificationsManager();
    NotificationsConfiguration.getNotificationsConfiguration().register(
            TrackerMessageBundle.message("notification.group.display.id"),
            NotificationDisplayType.STICKY_BALLOON);
    if (usageTrackerManager.isUsageTrackingAvailable() && !usageTrackerManager.hasUserRecordedTrackingPreference()) {
      UsageTrackerNotification.getInstance().showNotification();
    }
  }

  @Override
  public void disposeComponent() {
    // no-op
  }
}
