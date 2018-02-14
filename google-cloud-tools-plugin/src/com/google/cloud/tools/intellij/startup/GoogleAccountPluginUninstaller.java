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

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.ide.plugins.PluginManagerUISettings;
import com.intellij.ide.plugins.UninstallPluginAction;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.MessageDialogBuilder;

/**
 * Google Login used to be implemented as its own plugin that was distributed separately, but
 * depended on by the Cloud Tools plugin. We have now incorporated login directly into the Cloud
 * Tools plugin and this class checks whether the legacy Google Account plugin is still installed
 * and removes it, prompting the user to restart.
 *
 * <p>Keeping the old Google Account plugin installed causes problems such as duplicating the login
 * widget in the toolbar.
 */
public class GoogleAccountPluginUninstaller {

  private static final Logger LOGGER = Logger.getInstance(GoogleAccountPluginUninstaller.class);

  public void uninstallIfPresent() {
    IdeaPluginDescriptor accountPlugin =
        PluginManager.getPlugin(PluginId.findId("com.google.gct.login"));
    if (accountPlugin != null) {
      UsageTrackerProvider.getInstance().trackEvent(GctTracking.ACCOUNT_PLUGIN_DETECTED).ping();
      LOGGER.info("legacy google account plugin found.");
      accountPlugin.setEnabled(false);
      PluginManagerConfigurable managerConfigurable =
          new PluginManagerConfigurable(PluginManagerUISettings.getInstance());
      UninstallPluginAction.uninstall(managerConfigurable.getOrCreatePanel(), true, accountPlugin);
      UsageTrackerProvider.getInstance().trackEvent(GctTracking.ACCOUNT_PLUGIN_UNINSTALLED).ping();
      LOGGER.info(
          "legacy google account plugin has been disabled and uninstalled. This will take effect on"
              + " the next IDE restart.");
      if (MessageDialogBuilder.yesNo(
              GctBundle.message("account.plugin.removal.requires.restart.title"),
              GctBundle.message("account.plugin.removal.requires.restart.text"))
          .yesText(GctBundle.message("OK"))
          .noText(GctBundle.message("Cancel"))
          .isYes()) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.ACCOUNT_PLUGIN_RESTART_DIALOG_YES_ACTION)
            .ping();
        ApplicationManagerEx.getApplicationEx().restart(true);
      } else {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.ACCOUNT_PLUGIN_RESTART_DIALOG_NO_ACTION)
            .ping();
      }
    }
  }
}
