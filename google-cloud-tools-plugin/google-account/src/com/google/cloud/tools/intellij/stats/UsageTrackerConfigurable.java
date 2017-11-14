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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import javax.swing.JComponent;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a Usage Tracking item in the settings.
 */
public class UsageTrackerConfigurable implements Configurable {

  private UsageTrackerPanel usageTrackerPanel;
  private UsageTrackerManager usageTrackerManager;

  public UsageTrackerConfigurable() {
    usageTrackerManager = UsageTrackerManager.getInstance();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return AccountMessageBundle.message("settings.menu.item.usage.tracking.text");
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
