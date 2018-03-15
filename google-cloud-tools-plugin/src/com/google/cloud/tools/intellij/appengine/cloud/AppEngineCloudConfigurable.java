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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkConfigurable;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceUserSettings;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.ui.FontUtils;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.remoteServer.RemoteServerConfigurable;
import com.intellij.ui.components.JBLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/** GCP App Engine Cloud configuration UI. */
public class AppEngineCloudConfigurable extends RemoteServerConfigurable implements Configurable {

  private static final String MORE_INFO_URI_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/'>";
  private static final String MORE_INFO_URI_CLOSE_TAG = "</a>";
  private static final String PSEUDO_GOOGLE_SDK_LINK = "http://google-sdk/";

  private String displayName = GctBundle.message("appengine.name");
  private JPanel mainPanel;
  private JTextPane appEngineMoreInfoLabel;
  private JBLabel sdkValidationErrorLabel;

  /** Initialize the UI. */
  AppEngineCloudConfigurable() {
    // in case managed SDK is not available yet, provide direct link to SDK settings to allow
    // familiar SDK setup via App Engine Server panel.
    StringBuilder messageBuilder = new StringBuilder();
    if (!ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.MANAGED_SDK)) {
      messageBuilder.append(
          GctBundle.message("appengine.cloud.sdk.settings", PSEUDO_GOOGLE_SDK_LINK));
      messageBuilder.append("<p/>");
    }
    messageBuilder.append(
        GctBundle.message("appengine.more.info", MORE_INFO_URI_OPEN_TAG, MORE_INFO_URI_CLOSE_TAG));

    appEngineMoreInfoLabel.setText(messageBuilder.toString());
    appEngineMoreInfoLabel.addHyperlinkListener(
        new BrowserOpeningHyperLinkListener() {
          @Override
          public void hyperlinkUpdate(HyperlinkEvent event) {
            // check for specific settings pseudo-URL.
            if (event.getEventType() == EventType.ACTIVATED
                && event.getURL().toString().contains(PSEUDO_GOOGLE_SDK_LINK)) {
              ShowSettingsUtil.getInstance().showSettingsDialog(null, CloudSdkConfigurable.class);
              updateSdkValidationLabel();
            } else {
              super.hyperlinkUpdate(event);
            }
          }
        });
    appEngineMoreInfoLabel.setBackground(mainPanel.getBackground());
    FontUtils.convertStyledDocumentFontToDefault(appEngineMoreInfoLabel.getStyledDocument());

    updateSdkValidationLabel();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() {}

  @Override
  public void reset() {}

  /**
   * We don't need to test the connection if we know the cloud SDK, user, and project ID are valid.
   */
  @Override
  public boolean canCheckConnection() {
    return false;
  }

  @VisibleForTesting
  JTextPane getAppEngineMoreInfoLabel() {
    return appEngineMoreInfoLabel;
  }

  @VisibleForTesting
  JBLabel getSdkValidationErrorLabel() {
    return sdkValidationErrorLabel;
  }

  /**
   * Checks for custom SDK validation errors and shows error label if they exist. To be removed once
   * managed SDk feature is rolled out.
   */
  private void updateSdkValidationLabel() {
    if (!ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.MANAGED_SDK)) {
      String sdkValidationMessage =
          CloudSdkPanel.buildSdkMessage(
              CloudSdkServiceUserSettings.getInstance().getCustomSdkPath(), false);
      if (sdkValidationMessage != null) {
        sdkValidationErrorLabel.setText(sdkValidationMessage);
        sdkValidationErrorLabel.setVisible(true);
      } else {
        sdkValidationErrorLabel.setVisible(false);
      }
    } else {
      sdkValidationErrorLabel.setVisible(false);
    }
  }
}
