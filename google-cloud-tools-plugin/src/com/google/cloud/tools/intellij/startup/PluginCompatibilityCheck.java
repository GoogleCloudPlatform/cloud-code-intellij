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


import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerUISettings;
import com.intellij.ide.plugins.UninstallPluginAction;
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
import javax.swing.event.HyperlinkEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A plugin post startup activity which checks to ensure that the Google Cloud Tools and Account
 * plugins are running the same version. If there is a version mismatch, then a warning dialog is
 * displayed with a link to check for updates.
 */
public class PluginCompatibilityCheck implements StartupActivity {

  @Override
  public void runActivity(@NotNull Project project) {
    checkPluginCompatibility(project);
  }

  private void checkPluginCompatibility(@NotNull Project project) {
    IdeaPluginDescriptor accountPlugin =
        PluginManager.getPlugin(PluginId.findId("com.google.gct.login"));
    if (accountPlugin != null) {

      NotificationGroup notification = new NotificationGroup(
          GctBundle.message("account.plugin.removal.error.title"),
          NotificationDisplayType.BALLOON, true);

      notification.createNotification(
          GctBundle.message(
              "account.plugin.removal.error.message"),
          NotificationType.ERROR).notify(project);
      accountPlugin.setEnabled(false);
      PluginManagerConfigurable managerConfigurable = new PluginManagerConfigurable(
          PluginManagerUISettings.getInstance());
      UninstallPluginAction.uninstall(managerConfigurable.getOrCreatePanel(), true, accountPlugin);
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
