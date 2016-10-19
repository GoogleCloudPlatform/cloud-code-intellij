/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.ui.DisablePluginWarningDialog;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.popup.util.PopupUtil;
import com.intellij.openapi.wm.WindowManager;

import org.jetbrains.annotations.NotNull;

import java.awt.Window;

import javax.swing.event.HyperlinkEvent;

/**
 * Checks if conflicting any conflicting plugins are installed.
 */
public class ConflictingAppEnginePluginCheck {

  private static final String DEACTIVATE_LINK_HREF = "#deactivate";
  private static final String BUNDLED_PLUGIN_ID = "com.intellij.appengine";

  /**
   * Checks if the original bundled "App Engine Integration" plugin is installed. If so, the user is
   * notified to disable it.
   */
  public void notifyIfConflicting() {
    if (isPluginInstalled()) {
      notifyUser(getPlugin());
    }
  }

  private boolean isPluginInstalled() {
    IdeaPluginDescriptor pluginDescriptor = getPlugin();
    return pluginDescriptor != null && pluginDescriptor.isEnabled();
  }

  private IdeaPluginDescriptor getPlugin() {
    return PluginManager.getPlugin(PluginId.findId(BUNDLED_PLUGIN_ID));
  }

  private void notifyUser(@NotNull IdeaPluginDescriptor plugin) {
    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("plugin.conflict.error.title"),
            NotificationDisplayType.BALLOON,
            true);

    String errorMessage =
        new StringBuilder()
            .append("<p>")
            .append(GctBundle.message("plugin.conflict.error.detail", plugin.getName()))
            .append("</p>")
            .append("<br />")
            .append("<p>")
            .append(
                GctBundle.message(
                    "plugin.conflict.error.action",
                    "<a href=\"" + DEACTIVATE_LINK_HREF + "\">",
                    "</a>"))
            .append("</p>")
            .toString();

    notification
        .createNotification(
            GctBundle.message("plugin.conflict.error.title"),
            errorMessage,
            NotificationType.ERROR,
            new IdeaAppEnginePluginLinkListener(plugin))
        .notify(null);

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_OLD_PLUGIN_NOTIFICATION)
        .ping();
  }

  @VisibleForTesting
  static class IdeaAppEnginePluginLinkListener implements NotificationListener {

    private IdeaPluginDescriptor plugin;

    public IdeaAppEnginePluginLinkListener(@NotNull IdeaPluginDescriptor plugin) {
      this.plugin = plugin;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();

      if (DEACTIVATE_LINK_HREF.equals(href)) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_OLD_PLUGIN_NOTIFICATION_CLICK)
            .ping();
        showDisablePluginDialog();
        notification.hideBalloon();
      }
    }

    private void showDisablePluginDialog() {
      DisablePluginWarningDialog dialog = new DisablePluginWarningDialog(plugin.getPluginId(),
          PopupUtil.getActiveComponent());
      dialog.showAndDisablePlugin();
    }
  }
}
