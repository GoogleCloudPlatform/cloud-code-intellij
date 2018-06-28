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

import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Extension point for Cloud APIs, allowing access to UI and getting updates on cloud library
 * selection, module selection, and final library selection confirmation. See also {@link
 * CloudApiUiPresenter}.
 */
// TODO: move to core cloud API once dependency is inverted.
public interface CloudApiUiExtension {
  ExtensionPointName<CloudApiUiExtension> EP_NAME =
      new ExtensionPointName<>("com.google.gct.cloudapis.cloudApiUiExtension");

  /**
   * Called when add cloud libraries dialog is created and opened and base UI is ready. At this
   * point extension point is active.
   *
   * @param uiPresenter Presenter to access cloud API base UI.
   */
  void init(@NotNull CloudApiUiPresenter uiPresenter);

  /**
   * Callback on change in currently selected cloud library.
   *
   * @param currentCloudLibrary Cloud library selected or null if user de-selected library.
   * @param currentBomVersion BOM version. TODO:// to be removed, move to maven module.
   */
  void onCurrentCloudLibrarySelected(CloudLibrary currentCloudLibrary, String currentBomVersion);

  /**
   * Callback on module selection change.
   *
   * @param module Currently selected module.
   */
  void onModuleSelected(Module module);
}
