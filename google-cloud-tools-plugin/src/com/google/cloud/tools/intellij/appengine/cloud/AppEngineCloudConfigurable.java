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

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;
import com.google.cloud.tools.intellij.ui.BrowserOpeningHyperLinkListener;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.RemoteServerConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * GCP App Engine Cloud configuration UI.
 */
public class AppEngineCloudConfigurable extends RemoteServerConfigurable implements Configurable {

  private static final String MORE_INFO_URI_OPEN_TAG = "<a href='https://cloud.google.com/appengine/docs/flexible/'>";
  private static final String MORE_INFO_URI_CLOSE_TAG = "</a>";

  private String displayName = GctBundle.message("appengine.name");
  private JPanel mainPanel;
  private JTextPane appEngineFlexMoreInfoLabel;
  private CloudSdkPanel cloudSdkPanel;

  /**
   * Initialize the UI.
   */
  public AppEngineCloudConfigurable() {
    appEngineFlexMoreInfoLabel.setText(
        GctBundle.message(
            "appengine.flex.more.info",
            MORE_INFO_URI_OPEN_TAG,
            MORE_INFO_URI_CLOSE_TAG));
    appEngineFlexMoreInfoLabel.addHyperlinkListener(new BrowserOpeningHyperLinkListener());
    appEngineFlexMoreInfoLabel.setBackground(mainPanel.getBackground());
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
    return cloudSdkPanel.isModified();
  }

  /**
   * User's shouldn't be able to save a cloud configuration without a valid Cloud SDK configured.
   */
  @Override
  public void apply() throws ConfigurationException {
    if (StringUtil.isEmpty(cloudSdkPanel.getCloudSdkDirectory())
        || !CloudSdkUtil.containsCloudSdkExecutable(cloudSdkPanel.getCloudSdkDirectory())) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.cloudsdk.location.invalid.message"));
    } else {
      CloudSdkService.getInstance().setCloudSdkHomePath(cloudSdkPanel.getCloudSdkDirectory());
    }
  }

  @Override
  public void reset() {
    cloudSdkPanel.reset();
  }

  /**
   * We don't need to test the connection if we know the cloud SDK, user, and project ID are valid.
   */
  @Override
  public boolean canCheckConnection() {
    return false;
  }

  @VisibleForTesting
  CloudSdkPanel getCloudSdkPanel() {
    return cloudSdkPanel;
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  private void createUIComponents() {
    cloudSdkPanel = new CloudSdkPanel(CloudSdkService.getInstance());
  }
}
