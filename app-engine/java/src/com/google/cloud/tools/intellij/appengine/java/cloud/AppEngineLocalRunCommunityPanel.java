/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link SettingsEditor} UI panel for local-run of App Engine standard applications in Community
 * Edition.
 */
public class AppEngineLocalRunCommunityPanel
    extends SettingsEditor<AppEngineCommunityLocalServerRunConfiguration> {

  private JPanel panel;

  @Override
  protected void resetEditorFrom(@NotNull AppEngineCommunityLocalServerRunConfiguration s) {}

  @Override
  protected void applyEditorTo(@NotNull AppEngineCommunityLocalServerRunConfiguration s)
      throws ConfigurationException {}

  @NotNull
  @Override
  protected JComponent createEditor() {
    return panel;
  }
}
