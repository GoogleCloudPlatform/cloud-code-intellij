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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.login.util.TrackerMessageBundle;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 */
public class UsageTrackerPanel {

  private static final Logger LOG = Logger.getInstance(GoogleSettingsConfigurable.class);

  private JCheckBox enableUsageTrackerBox;
  private JPanel mainPanel;
  private JLabel privacyPolicyText;

  public static final String PRIVACY_POLICY_URL = "http://www.google.com/policies/privacy/";
  private UsageTrackerManager usageTrackerManager;

  public UsageTrackerPanel(UsageTrackerManager usageTrackerManager) {
    this.usageTrackerManager = usageTrackerManager;

    enableUsageTrackerBox.setSelected(usageTrackerManager.hasUserOptedIn());
    privacyPolicyText.setText(TrackerMessageBundle.message(
        "settings.privacy.policy.comment",
        PRIVACY_POLICY_URL));


    // Disable checkbox if usage tracker property has not been configured
    if (!usageTrackerManager.isUsageTrackingAvailable()) {
      enableUsageTrackerBox.setEnabled(false);
    }

    privacyPolicyText.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent me) {
        privacyPolicyText.setCursor(new Cursor(Cursor.HAND_CURSOR));
      }

      public void mouseExited(MouseEvent me) {
        privacyPolicyText.setCursor(Cursor.getDefaultCursor());
      }

      public void mouseClicked(MouseEvent me) {
        try {
          BrowserUtil.browse(new URL(PRIVACY_POLICY_URL));
        } catch (MalformedURLException mue) {
          LOG.error(mue);
        }
      }
    });
  }

  public boolean isModified() {
    return usageTrackerManager.isTrackingEnabled()
        && usageTrackerManager.hasUserOptedIn() != enableUsageTrackerBox.isSelected();
  }

  public void apply() {
    if (usageTrackerManager.isUsageTrackingAvailable()) {
      usageTrackerManager.setTrackingPreference(enableUsageTrackerBox.isSelected());
    }
  }

  public void reset() {
    if (usageTrackerManager.isUsageTrackingAvailable()) {
      enableUsageTrackerBox.setSelected(usageTrackerManager.hasUserOptedIn());
    }
  }

  @NotNull
  public JPanel getComponent() {
    return mainPanel;
  }
}
