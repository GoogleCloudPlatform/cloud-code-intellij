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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.Plugins;

import com.intellij.diagnostic.errordialog.DisablePluginWarningDialog;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.WindowManager;

import org.jetbrains.annotations.NotNull;

import java.awt.Window;

import javax.swing.event.HyperlinkEvent;

/**
 * A plugin post startup activity which checks if the bundled (now deprecated) app engine plugin is
 * running. If so, the user is notified to disable it.
 */
public class ConflictingAppEnginePluginCheck implements StartupActivity {

  private static final String DEACTIVATE_LINK_HREF = "#deactivate";
  private static final String BUNDLED_PLUGIN_ID = "com.intellij.appengine";

  private Plugins plugins;

  public ConflictingAppEnginePluginCheck() {
    plugins = new Plugins();
  }

  @Override
  public void runActivity(@NotNull Project project) {
    if (plugins.isPluginInstalled(BUNDLED_PLUGIN_ID)) {
      notifyUser(project, plugins.getPluginById(BUNDLED_PLUGIN_ID));
    }
  }

  private void notifyUser(@NotNull Project project, @NotNull IdeaPluginDescriptor plugin) {
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
            new IdeaAppEnginePluginLinkListener(project, plugin))
        .notify(project);
  }

  private static class IdeaAppEnginePluginLinkListener implements NotificationListener {

    private Project project;
    private IdeaPluginDescriptor plugin;

    public IdeaAppEnginePluginLinkListener(@NotNull final Project project,
        @NotNull IdeaPluginDescriptor plugin) {
      this.project = project;
      this.plugin = plugin;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();

      if (DEACTIVATE_LINK_HREF.equals(href)) {
        showDisablePluginDialog(project);
        notification.hideBalloon();
      }
    }

    private void showDisablePluginDialog(@NotNull Project project) {
      Application app = ApplicationManager.getApplication();
      Window parent = WindowManager.getInstance().suggestParentWindow(project);

      DisablePluginWarningDialog dialog =
          new DisablePluginWarningDialog(parent, plugin.getName(), true, app.isRestartCapable());
      dialog.show();

      String pluginId = plugin.getPluginId().getIdString();

      switch (dialog.getExitCode()) {
        case DisablePluginWarningDialog.CANCEL_EXIT_CODE:
          return;
        case DisablePluginWarningDialog.DISABLE_EXIT_CODE:
          PluginManager.disablePlugin(pluginId);
          break;
        case DisablePluginWarningDialog.DISABLE_AND_RESTART_EXIT_CODE:
          PluginManager.disablePlugin(pluginId);
          app.restart();
          break;
        default:
      }
    }
  }
}
