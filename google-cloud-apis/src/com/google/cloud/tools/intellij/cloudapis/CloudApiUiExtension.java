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
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extension point for Cloud APIs, allowing access to UI and getting updates on cloud library
 * selection, module selection, and final library selection confirmation. See also {@link
 * CloudApiUiPresenter}.
 */
public interface CloudApiUiExtension {
  ExtensionPointName<CloudApiUiExtension> EP_NAME =
      new ExtensionPointName<>("com.google.gct.cloudapis.cloudApiUiExtension");

  /** Possible locations for custom extension UI components. */
  enum EXTENSION_UI_COMPONENT_LOCATION {
    /** bottom line 1 - under module label */
    BOTTOM_LINE_1,
    /** bottom line 2 - under module combo box */
    BOTTOM_LINE_2
  }

  /**
   * Called when add cloud libraries dialog is created and opened and base UI is ready. At this
   * point extension point is active and custom UI components can be injected.
   */
  @NotNull
  Map<EXTENSION_UI_COMPONENT_LOCATION, JComponent> createCustomUiComponents();

  /**
   * Callback on change in currently selected cloud library.
   *
   * @param currentCloudLibrary Cloud library selected or null if user de-selected library.
   */
  void onCloudLibrarySelection(@Nullable CloudLibrary currentCloudLibrary);

  /**
   * Callback on module selection change.
   *
   * @param module Currently selected module.
   */
  void onModuleSelection(Module module);

  /**
   * Callback after user confirmed adding cloud libraries, last step of the Cloud API dialog. At
   * this point extension can assume libraries are added to the project and module.
   *
   * @param libraries List of added libraries.
   * @param module Module where libraries were added.
   */
  void onCloudLibrariesAddition(@NotNull Set<CloudLibrary> libraries, @NotNull Module module);
}
