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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.util.Collection;
import java.util.Optional;
import javax.swing.Icon;
import org.jetbrains.annotations.Nullable;

/**
 * Presenter for core Cloud API UI, allows accessing UI points for libraries for extension points to
 * update, including documentation links and version info.
 */
public interface CloudApiUiPresenter {

  /**
   * Obtains active instance of {@link CloudApiUiPresenter} for currently opened and active Cloud
   * API selection dialog.
   */
  static CloudApiUiPresenter getInstance() {
    return ServiceManager.getService(CloudApiUiPresenter.class);
  }

  /** Returns current IDE project. */
  Project getProject();

  /** Obtains currently selected module for current IDE project. */
  Module getSelectedModule();

  /**
   * Adds links to some documentation page for the currently selected library. If no library is
   * selected, nothing is done.
   *
   * @param links Collection of optional prepared links in HTML format, prepared by {@link
   *     GoogleCloudApiDetailsPanel#makeLink(String, String)}
   */
  void addCloudLibraryLinks(Collection<Optional<String>> links);

  /**
   * Updates version information for the currently selected library. If no library is selected,
   * nothing is done.
   *
   * @param text Text for version label. Null text does nothing (old text retained).
   * @param icon Icon for version label. Null clears current icon.
   */
  void updateCloudLibraryVersionLabel(@Nullable String text, @Nullable Icon icon);
}
