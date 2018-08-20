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

import com.google.cloud.tools.intellij.cloudapis.CloudApiUiExtension.EXTENSION_UI_COMPONENT_LOCATION;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  private Project project;
  private AddCloudLibrariesDialog addCloudLibrariesDialog;
  private GoogleCloudApiSelectorPanel cloudApiSelectorPanel;

  private CloudApiUiExtension[] cloudApiUiExtensions;

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public Module getSelectedModule() {
    return cloudApiSelectorPanel.getSelectedModule();
  }

  @Override
  public void setCloudApiDialogTitle(@NotNull String title) {
    addCloudLibrariesDialog.setTitle(title);
  }

  @Override
  public void setCloudApiDialogOkButtonText(@NotNull String text) {
    addCloudLibrariesDialog.updateOKButtonText(text);
  }

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
  void init(
      Project project,
      @NotNull AddCloudLibrariesDialog addCloudLibrariesDialog,
      @NotNull GoogleCloudApiSelectorPanel cloudApiSelectorPanel) {
    this.project = project;
    this.addCloudLibrariesDialog = addCloudLibrariesDialog;
    this.cloudApiSelectorPanel = cloudApiSelectorPanel;

    cloudApiUiExtensions = CloudApiUiExtension.EP_NAME.getExtensions();
    for (CloudApiUiExtension uiExtension : cloudApiUiExtensions) {
      Map<EXTENSION_UI_COMPONENT_LOCATION, JComponent> customComponents =
          uiExtension.createCustomUiComponents();
      cloudApiSelectorPanel.createExtensionUiComponents(customComponents);
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
                                cloudApiSelectorPanel.getDetailsPanel().getCurrentCloudLibrary())));
  }

  /**
   * See {@link
   * com.google.cloud.tools.intellij.cloudapis.CloudApiUiExtension#onCloudLibrariesAddition(java.util.Set,
   * com.intellij.openapi.module.Module)}
   */
  void notifyCloudLibrariesAddition(@NotNull Set<CloudLibrary> libraries, @NotNull Module module) {
    if (cloudApiUiExtensions != null) {
      Stream.of(cloudApiUiExtensions)
          .forEach(uiExtension -> uiExtension.onCloudLibrariesAddition(libraries, module));
    }
  }
}
