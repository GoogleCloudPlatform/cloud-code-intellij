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

package com.google.cloud.tools.intellij;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.net.UrlEscapers;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAwareAction;
import java.text.MessageFormat;

/** Adds a Cloud Tools menu shortcut link to the GitHub issue creation page. */
public class CloudToolsFeedbackAction extends DumbAwareAction {

  private static final String NEW_ISSUE_URL =
      new PropertiesFileFlagReader().getFlagString("plugin.github.new.issue.url");

  // should be kept up to date with .github/ISSUE_TEMPLATE.md
  private static final String BODY_TEMPLATE =
      "(Please ensure you are running the latest version of Cloud Tools for IntelliJ with _Help > Check for Updates_)\n"
          + "- Cloud Tools for IntelliJ version: {0}\n"
          + "- Google Cloud SDK version: {1}\n"
          + "- OS: {2} {3}\n"
          + "\n"
          + "**What did you do?**\n"
          + "\n"
          + "**What did you expect to see?**\n"
          + "\n"
          + "**What did you see instead?**\n"
          + "\n"
          + "(screenshots are helpful)\n"
          + "_Feel free to deviate from this template as needed, especially if you are submitting a feature request._";

  public CloudToolsFeedbackAction() {
    super(
        GctBundle.message("plugin.tools.menu.item.feedback.title"),
        GctBundle.message("plugin.tools.menu.item.feedback.description"),
        GoogleCloudToolsIcons.FEEDBACK);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    BrowserLauncher.getInstance().browse(formatUrl(), /* browser= */ null, /* project= */ null);
  }

  private static String formatUrl() {
    String pluginVersion = ServiceManager.getService(PluginInfoService.class).getPluginVersion();

    String issueBody =
        MessageFormat.format(
            BODY_TEMPLATE,
            pluginVersion,
            getCloudSdkVersion(),
            System.getProperty("os.name"),
            System.getProperty("os.version"));

    return NEW_ISSUE_URL + "?body=" + UrlEscapers.urlFormParameterEscaper().escape(issueBody);
  }

  private static String getCloudSdkVersion() {
    try {
      CloudSdk sdk =
          new CloudSdk.Builder().sdkPath(CloudSdkService.getInstance().getSdkHomePath()).build();
      return sdk.getVersion().toString();
    } catch (AppEngineException aee) {
      return "";
    }
  }
}
