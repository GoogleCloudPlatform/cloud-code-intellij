/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.server.integration;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkPanel;

import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentData;
import com.intellij.javaee.appServerIntegrations.ApplicationServerPersistentDataEditor;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * @author nik
 */
public class AppEngineServerEditor extends
    ApplicationServerPersistentDataEditor<ApplicationServerPersistentData> {

  private CloudSdkPanel cloudSdkPanel;

  public AppEngineServerEditor() {
    cloudSdkPanel = new CloudSdkPanel();
  }

  protected void resetEditorFrom(ApplicationServerPersistentData data) {
    cloudSdkPanel.reset();
  }

  protected void applyEditorTo(ApplicationServerPersistentData data) {
    try {
      cloudSdkPanel.apply();
    } catch (ConfigurationException ce) {
      // do nothing
    }
  }

  @NotNull
  protected JComponent createEditor() {
    return cloudSdkPanel.getComponent();
  }
}
