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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Creates a Cloud SDK menu item for configuring the path to the Cloud SDK.
 */
public class CloudSdkConfigurable implements SearchableConfigurable {

  private CloudSdkPanel cloudSdkPanel;
  private CloudSdkService cloudSdkService;

  public CloudSdkConfigurable() {
    cloudSdkService = CloudSdkService.getInstance();
  }

  @NotNull
  @Override
  public String getId() {
    return "google.settings.sdk";
  }

  @Nullable
  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Cloud SDK";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (cloudSdkPanel == null) {
      cloudSdkPanel = new CloudSdkPanel(cloudSdkService);
    }

    return cloudSdkPanel.getComponent();
  }

  @Override
  public boolean isModified() {
    return cloudSdkPanel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    cloudSdkPanel.apply();
  }

  @Override
  public void reset() {
    cloudSdkPanel.reset();
  }

  @Override
  public void disposeUIResources() {

  }
}
