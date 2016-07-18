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
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A plugin post startup activity which checks if the bundled (now deprecated) app engine plugin is
 * running. If so, the user is notified to disable it.
 */
public class ConflictingAppEnginePluginCheck implements StartupActivity {

  private static final String DEACTIVATE_LINK_HREF = "#deactivate";

  @Override
  public void runActivity(@NotNull Project project) {
    IdeaPluginDescriptor bundledPlugin =
        PluginManager.getPlugin(PluginId.findId("com.intellij.appengine"));

    if (pluginIsActive(bundledPlugin)) {
      notifyUser(project, bundledPlugin.getName());
    }
  }

  private boolean pluginIsActive(IdeaPluginDescriptor plugin) {
    return plugin != null && plugin.isEnabled();
  }

  private void notifyUser(@NotNull Project project, String pluginName) {
    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("plugin.conflict.error.title"),
            NotificationDisplayType.BALLOON,
            true);

    String errorMessage =
        new StringBuilder()
            .append("<p>")
            .append(GctBundle.message("plugin.conflict.error.detail", pluginName))
            .append("</p>")
            .append("<p>")
            .append(GctBundle.message("plugin.conflict.error.action",
                "<a href=\"" + DEACTIVATE_LINK_HREF + "\">",
                "</a>"))
            .append("</p>")
            .toString();

    notification.createNotification(
        GctBundle.message("plugin.conflict.error.title"),
        errorMessage,
        NotificationType.ERROR,
        new IdeaAppEnginePluginLinkListener(project))
        .notify(project);
  }

  private static class IdeaAppEnginePluginLinkListener implements NotificationListener {

    private Project project;

    public IdeaAppEnginePluginLinkListener(@NotNull final Project project) {
      this.project = project;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
      String href = event.getDescription();

      if (DEACTIVATE_LINK_HREF.equals(href)) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, PluginManagerConfigurable.class);
      }
    }
  }
}
