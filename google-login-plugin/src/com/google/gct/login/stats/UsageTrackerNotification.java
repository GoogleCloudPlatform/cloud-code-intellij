package com.google.gct.login.stats;

import com.intellij.notification.*;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

/**
 * Creates notification to allow user to opt in/out of usage traking for Cloud Tools plugin.
 */
public class UsageTrackerNotification {
    public static String GROUP_DISPLAY_ID = "Cloud Tools Plugin Usage Statistics";

    private  UsageTrackerNotification () {
    }

    /**
     * Displays the notification to allow user to opt in/out to usage tracking.
     */
    public static void showNotification() {
        String title = "Help improve the Cloud Tools plugin by sending usage statistics to Google";
        String content = "<html>Please click <a href='allow'>I agree</a> if you want to help make"
                + " the Cloud Tools plugin better or <a href='decline'>I don't agree</a> "
                + "otherwise. <a href='settings'>more...</a></html>";

        NotificationListener listener = new NotificationListener() {
            @Override
            public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    final String description = event.getDescription();
                    if ("allow".equals(description)) {
                        UsageTrackerManager.setOptIn(true);
                        notification.expire();
                    }
                    else if ("decline".equals(description)) {
                        UsageTrackerManager.setOptIn(false);
                        notification.expire();
                    }
                    else if ("settings".equals(description)) {
                        final ShowSettingsUtil util = ShowSettingsUtil.getInstance();
                        IdeFrame ideFrame = WindowManagerEx.getInstanceEx().findFrameFor(null);
                        util.editConfigurable((JFrame)ideFrame, new GctConfigurable());
                        notification.expire();
                    }
                }

            }
        };

        Notification notification = new Notification(GROUP_DISPLAY_ID, title, content,
                NotificationType.INFORMATION, listener);
        Notifications.Bus.notify(notification);
    }
}
