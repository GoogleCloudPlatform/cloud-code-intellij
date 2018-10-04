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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.stackdriver.debugger.ui.CloudDebugRunConfigurationPanel;
import com.google.common.base.Strings;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import java.util.Optional;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

/**
 * Creates the UI to change Run Configuration settings and applies those to our {@link
 * CloudDebugRunConfiguration}.
 */
public class CloudDebugSettingsEditor extends SettingsEditor<CloudDebugRunConfiguration> {

  private final CloudDebugRunConfigurationPanel settingsPanel;

  CloudDebugSettingsEditor(Project project) {
    settingsPanel = new CloudDebugRunConfigurationPanel(project);
  }

  @Override
  protected void applyEditorTo(@NotNull CloudDebugRunConfiguration runConfiguration) {
    runConfiguration.setCloudProjectId(
        Optional.ofNullable(settingsPanel.getSelectedCloudProject())
            .map(CloudProject::projectId)
            .orElse(null));
    runConfiguration.setGoogleUsername(
        Optional.ofNullable(settingsPanel.getSelectedCloudProject())
            .map(CloudProject::googleUsername)
            .orElse(null));
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return settingsPanel.getMainPanel();
  }

  @Override
  protected void resetEditorFrom(@NotNull CloudDebugRunConfiguration runConfiguration) {
    String projectId = runConfiguration.getCloudProjectId();
    // google username may be empty from previous revisions of configurations, should not be null.
    String googleUsername = Optional.ofNullable(runConfiguration.getGoogleUsername()).orElse("");
    if (!Strings.isNullOrEmpty(projectId)) {
      settingsPanel.setSelectedCloudProject(
          CloudProject.create(
              // TODO(ivanporty) no project name in CloudDebugRunConfiguration.
              projectId /* as name */, projectId, null /* project number */, googleUsername));
    } else {
      settingsPanel.loadActiveCloudProject();
    }
  }
}
