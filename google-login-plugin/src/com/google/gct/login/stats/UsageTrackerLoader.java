package com.google.gct.login.stats;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Checks to see if user has opted in/out of usage tracking for the Cloud Tools plugin.
 * If user has not, it notifies user to opt in/out of usage tracking, otherwise it does nothing.
 */
public class UsageTrackerLoader implements ApplicationComponent {
    @Override
    public void initComponent() {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return;
        }

        if (!UsageTrackerManager.isTrackingConfigured()) {
            // Ensure that the notification manager (also an application component) is registered first;
            // otherwise this component's initComponent() call will fire a notification event bus
            // to show the opt-in dialog, but the notification component may not yet have been initialized
            // and is therefore not subscribed and listening.
            NotificationsManager.getNotificationsManager();
            NotificationsConfiguration.getNotificationsConfiguration().register(
                    UsageTrackerNotification.GROUP_DISPLAY_ID,
                    NotificationDisplayType.STICKY_BALLOON);

            UsageTrackerNotification.showNotification();
        }
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    @Override
    public String getComponentName() {
        return UsageTrackerLoader.class.getName();
    }
}
