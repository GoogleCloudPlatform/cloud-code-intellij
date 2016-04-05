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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.login.util.TrackerMessageBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation of {@code ApplicationConfigurable} extension that provides a
 * Google Cloud Tools tab in the "Settings" dialog.
 */
public class GoogleSettingsConfigurable implements SearchableConfigurable {
    private JCheckBox enableUsageTrackerBox;
    private static final Logger LOG = Logger.getInstance(GoogleSettingsConfigurable.class);
    public static final String PRIVACY_POLICY_URL = "http://www.google.com/policies/privacy/";
    private UsageTrackerManager usageTrackerManager;

    public GoogleSettingsConfigurable() {
        usageTrackerManager = UsageTrackerManager.getInstance();
    }

    public GoogleSettingsConfigurable(UsageTrackerManager trackerManager) {
        usageTrackerManager = trackerManager;
    }

    @NotNull
    @Override
    public String getId() {
        return "google.usage.tracker";
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Google";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        if (usageTrackerManager.isUsageTrackingAvailable()) {
            // Add the Usage Tracker Box
            JPanel usageTrackerGroup = creatUsageTrackerComponent();
            mainPanel.add(usageTrackerGroup, BorderLayout.NORTH);
        }
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return usageTrackerManager.hasUserOptedIn() != enableUsageTrackerBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        usageTrackerManager.setTrackingPreference(enableUsageTrackerBox.isSelected());
    }

    @Override
    public void reset() {
        enableUsageTrackerBox.setSelected(usageTrackerManager.hasUserOptedIn());
    }

    @Override
    public void disposeUIResources() {
        enableUsageTrackerBox = null;
    }

    private JPanel creatUsageTrackerComponent() {
        enableUsageTrackerBox = new JCheckBox(TrackerMessageBundle.message("settings.enable.tracker.text"));
        enableUsageTrackerBox.setSelected(usageTrackerManager.hasUserOptedIn());

        // Disable checkbox if usage tracker property has not been configured------
        if (!usageTrackerManager.isUsageTrackingAvailable()) {
            enableUsageTrackerBox.setEnabled(false);
        }

        final JLabel privacyPolicyText = new JLabel(
                TrackerMessageBundle.message("settings.privacy.policy.comment", PRIVACY_POLICY_URL));
        privacyPolicyText.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent me) {
                privacyPolicyText.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent me) {
                privacyPolicyText.setCursor(Cursor.getDefaultCursor());
            }
            public void mouseClicked(MouseEvent me)
            {
                try {
                    BrowserUtil.browse(new URL(PRIVACY_POLICY_URL));
                }
                catch(MalformedURLException e) {
                    LOG.error(e);
                }
            }
        });

        JPanel usageTrackerGroup = new JPanel(new BorderLayout());
        usageTrackerGroup.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Usage Tracker"));
        usageTrackerGroup.add(enableUsageTrackerBox, BorderLayout.NORTH);
        usageTrackerGroup.add(privacyPolicyText, BorderLayout.SOUTH);
        return usageTrackerGroup;
    }
}
