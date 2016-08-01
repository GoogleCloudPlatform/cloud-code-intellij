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

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Creates a Usage Tracking item in the settings.
 */
public class UsageTrackerConfigurable implements SearchableConfigurable {

  private UsageTrackerPanel usageTrackerPanel;
  private UsageTrackerManager usageTrackerManager;

  public UsageTrackerConfigurable() {
    usageTrackerManager = UsageTrackerManager.getInstance();
  }

  @NotNull
  @Override
  public String getId() {
    return "google.settings.tracker";
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Usage Tracking";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (usageTrackerPanel == null) {
      usageTrackerPanel = new UsageTrackerPanel(usageTrackerManager);
    }
    return usageTrackerPanel.getComponent();
  }

  @Override
  public boolean isModified() {
    return usageTrackerPanel != null && usageTrackerPanel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    if (usageTrackerPanel != null) {
      usageTrackerPanel.apply();
    }
  }

  @Override
  public void reset() {
    if (usageTrackerPanel != null) {
      usageTrackerPanel.reset();
    }
  }

  @Override
  public void disposeUIResources() {
    usageTrackerPanel = null;
  }
}