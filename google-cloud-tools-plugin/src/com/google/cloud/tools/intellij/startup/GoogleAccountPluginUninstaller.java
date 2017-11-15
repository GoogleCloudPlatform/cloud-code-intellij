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

import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;
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
 * A plugin post startup activity which checks to ensure that the Google Cloud Tools and Account
 * plugins are running the same version. If there is a version mismatch, then a warning dialog is
 * displayed with a link to check for updates.
 */
public class GoogleAccountPluginUninstaller {

  private static final Logger LOGGER = Logger.getInstance(GoogleAccountPluginUninstaller.class);

  public void uninstallIfPresent() {

    IdeaPluginDescriptor accountPlugin =
        PluginManager.getPlugin(PluginId.findId("com.google.gct.login"));
    if (accountPlugin != null) {
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.ACCOUNT_PLUGIN_DETECTED)
          .ping();
      LOGGER.info("legacy google account plugin found.");
      accountPlugin.setEnabled(false);
      PluginManagerConfigurable managerConfigurable =
          new PluginManagerConfigurable(PluginManagerUISettings.getInstance());
      UninstallPluginAction.uninstall(managerConfigurable.getOrCreatePanel(), true, accountPlugin);
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.ACCOUNT_PLUGIN_UNINSTALLED)
          .ping();
      LOGGER.info(
          "legacy google account plugin has been disabled and uninstalled. This will take effect on"
              + " the next IDE restart.");
      if (MessageDialogBuilder
          .yesNo(GctBundle.message("account.plugin.removal.requires.restart.title"),
              GctBundle.message("account.plugin.removal.requires.restart.text"))
          .yesText(GctBundle.message("OK"))
          .noText(GctBundle.message("Cancel"))
          .isYes()) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.ACCOUNT_PLUGIN_RESTART_DIALOG_YES_ACTION)
            .ping();
        ApplicationManagerEx.getApplicationEx().restart(true);
        return; // presumably we never get here, but just to make sure we never ping no and yes.
      }
      UsageTrackerProvider.getInstance()
          .trackEvent(GctTracking.ACCOUNT_PLUGIN_RESTART_DIALOG_NO_ACTION)
          .ping();
    }
  }
}
