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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default {@link CloudApiUiPresenter} implementation giving access to base cloud API UI for
 * extension points.
 */
public class DefaultCloudApiUiPresenter implements CloudApiUiPresenter {

  private GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

  private CloudApiUiExtension[] cloudApiUiExtensions;

  @Override
  public void addCloudLibraryLinks(Collection<Optional<String>> links) {
    cloudApiSelectorPanel.getDetailsPanel().addCloudLibraryLinks(links);
  }

  @Override
  public void updateCloudLibraryVersionLabel(@Nullable String text, @Nullable Icon icon) {
    if (text != null) {
      cloudApiSelectorPanel.getVersionLabel().setText(text);
    }
    cloudApiSelectorPanel.getVersionLabel().setIcon(icon);
  }

  /**
   * Inits the presenter when add libraries dialog is created, creates all extension points and adds
   * necessary event listeners and handlers.
   */
  void init(@NotNull GoogleCloudApiSelectorPanel cloudApiSelectorPanel) {
    this.cloudApiSelectorPanel = cloudApiSelectorPanel;

    cloudApiUiExtensions = CloudApiUiExtension.EP_NAME.getExtensions();
    for (CloudApiUiExtension uiExtension : cloudApiUiExtensions) {
      // TODO: will be implemented in the next PR.
      Collection<JComponent> customComponents = uiExtension.createCustomUiComponents();
    }
    cloudApiSelectorPanel.addModuleSelectionListener(
        e ->
            Stream.of(cloudApiUiExtensions)
                .forEach(
                    uiExtension ->
                        uiExtension.onModuleSelection(cloudApiSelectorPanel.getSelectedModule())));
    cloudApiSelectorPanel
        .getCloudLibrariesTable()
        .getSelectionModel()
        .addListSelectionListener(
            e ->
                Stream.of(cloudApiUiExtensions)
                    .forEach(
                        uiExtension ->
                            uiExtension.onCloudLibrarySelection(
                                cloudApiSelectorPanel.getDetailsPanel().getCurrentCloudLibrary(),
                                cloudApiSelectorPanel.getSelectedBomVersion().orElse(null))));
  }
}
