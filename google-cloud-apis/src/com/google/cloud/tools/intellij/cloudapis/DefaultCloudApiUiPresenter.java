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

package com.google.cloud.tools.intellij.cloudapis;

import com.google.cloud.tools.intellij.cloudapis.maven.CloudApiUiPresenter;
import com.google.cloud.tools.intellij.cloudapis.maven.MavenCloudApiUiExtension;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultCloudApiUiPresenter implements CloudApiUiPresenter {

  private GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

  private MavenCloudApiUiExtension mavenCloudApiUiExtension;

  @Override
  public void addCloudLibraryDocumentationLink(@NotNull String link) {
    cloudApiSelectorPanel.getDetailsPanel().addCloudLibraryDocumentationLink(link);
  }

  @Override
  public void updateCloudLibraryVersionLabel(@Nullable String text, @Nullable Icon icon) {
    if (text != null) {
      cloudApiSelectorPanel.getVersionLabel().setText(text);
    }
    cloudApiSelectorPanel.getVersionLabel().setIcon(icon);
  }

  void init(GoogleCloudApiSelectorPanel cloudApiSelectorPanel) {
    this.cloudApiSelectorPanel = cloudApiSelectorPanel;

    mavenCloudApiUiExtension = new MavenCloudApiUiExtension();
    mavenCloudApiUiExtension.init(this);
    cloudApiSelectorPanel.addModuleSelectionListener(
        e -> mavenCloudApiUiExtension.onModuleSelected(cloudApiSelectorPanel.getSelectedModule()));
    cloudApiSelectorPanel
        .getCloudLibrariesTable()
        .getSelectionModel()
        .addListSelectionListener(
            e ->
                mavenCloudApiUiExtension.onCurrentCloudLibrarySelected(
                    cloudApiSelectorPanel.getDetailsPanel().getCurrentCloudLibrary(),
                    cloudApiSelectorPanel.getSelectedBomVersion().orElse(null)));
  }
}
