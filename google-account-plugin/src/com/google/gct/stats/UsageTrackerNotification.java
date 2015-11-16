package com.google.gct.stats;

import com.google.gct.login.util.TrackerMessageBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Creates notification to allow user to opt in/out of usage traking for Cloud Tools plugin.
 */
public class UsageTrackerNotification {
    private static final Logger LOG = Logger.getInstance(UsageTrackerNotification.class);
    private static final UsageTrackerNotification INSTANCE = new UsageTrackerNotification();
    private final UsageTrackerManager usageTrackerManager;

    private  UsageTrackerNotification () {
        usageTrackerManager = UsageTrackerManager.getInstance();
    }

    public static UsageTrackerNotification getInstance() {
        return INSTANCE;
    }

    public void showNotification() {
        NotificationListener listener = new NotificationListener() {
            @Override
            public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    final String description = event.getDescription();
                    if ("allow".equals(description)) {
                        usageTrackerManager.setTrackingPreference(true);
                        notification.expire();
                    }
                    else if ("decline".equals(description)) {
                        UsageTrackerManager usageTrackerManager = UsageTrackerManager.getInstance();
                        usageTrackerManager.setTrackingPreference(false);
                        notification.expire();
                    }
                    else if ("policy".equals(description)) {
                        try {
                            BrowserUtil.browse(new URL(GoogleSettingsConfigurable.PRIVACY_POLICY_URL));
                        } catch (MalformedURLException e) {
                            LOG.error(e);
                        }
                        notification.expire();
                    }
                    else if ("settings".equals(description)) {
                        final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
                        IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
                        util.editConfigurable((JFrame)ideFrame, new GoogleSettingsConfigurable());
                        notification.expire();
                    }
                }

            }
        };

        Notification notification = new Notification(TrackerMessageBundle.message("notification.group.display.id"),
            TrackerMessageBundle.message("notification.popup.title"),
            TrackerMessageBundle.message("notification.popup.content"),
            NotificationType.INFORMATION, listener);
        Notifications.Bus.notify(notification);
    }
}
