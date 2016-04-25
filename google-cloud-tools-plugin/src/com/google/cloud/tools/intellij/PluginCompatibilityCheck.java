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
import com.intellij.openapi.updateSettings.impl.UpdateChecker;

import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

/**
 * A plugin post startup activity which checks to ensure that the Google Cloud Tools
 * and Account plugins are running the same version. If there is a version mismatch, then a
 * warning dialog is displayed with a link to check for updates.
 */
public class PluginCompatibilityCheck implements StartupActivity {

  @Override
  public void runActivity(@NotNull Project project) {
    checkPluginCompatibility(project);
  }

  private void checkPluginCompatibility(@NotNull Project project) {
    IdeaPluginDescriptor cloudToolsPlugin =
        PluginManager.getPlugin(PluginId.findId("com.google.gct.core"));
    IdeaPluginDescriptor accountPlugin =
        PluginManager.getPlugin(PluginId.findId("com.google.gct.login"));

    if (cloudToolsPlugin == null || accountPlugin == null) {
      return;
    }

    String cloudToolsPluginVersion = cloudToolsPlugin.getVersion();
    String accountPluginVersion = accountPlugin.getVersion();

    if (!accountPluginVersion.equals(cloudToolsPluginVersion)) {
      NotificationGroup notification = new NotificationGroup(
          GctBundle.message("plugin.compatibility.error.title"),
          NotificationDisplayType.BALLOON, true);

      StringBuilder errorMessage = new StringBuilder();

      errorMessage.append("<p>");
      errorMessage.append(GctBundle.message("plugin.compatibility.error.message"));
      errorMessage.append("<p/>");


      errorMessage.append("<ul>");
      errorMessage.append("<li>");
      errorMessage.append(
          GctBundle.message(
              "plugin.compatibility.error.name.and.version",
              cloudToolsPlugin.getName(),
              cloudToolsPluginVersion));
      errorMessage.append("</li>");
      errorMessage.append("<li>");
      errorMessage.append(
          GctBundle.message(
              "plugin.compatibility.error.name.and.version",
              accountPlugin.getName(),
              accountPluginVersion));
      errorMessage.append("</li>");
      errorMessage.append("</ul>");

      errorMessage.append("<br />");

      errorMessage.append("<p>");
      errorMessage.append(
          GctBundle.message(
              "plugin.compatibility.error.update.link", "<a href=\"#update\">", "</a>"));
      errorMessage.append("</p>");

      notification.createNotification(
          GctBundle.message("plugin.compatibility.error.title"),
          errorMessage.toString(),
          NotificationType.ERROR, new PluginCompatibilityLinkListener(project)).notify(project);
    }
  }

  private static class PluginCompatibilityLinkListener implements NotificationListener {
    private Project project;

    public PluginCompatibilityLinkListener(@NotNull final Project project) {
      this.project = project;
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification,
        @NotNull HyperlinkEvent event) {
      String href = event.getDescription();

      if ("#update".equals(href)) {
        UpdateChecker.updateAndShowResult(project, null);
      } else if ("#manage".equals(href)) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project,
            PluginManagerConfigurable.class);
      }
    }
  }
}
