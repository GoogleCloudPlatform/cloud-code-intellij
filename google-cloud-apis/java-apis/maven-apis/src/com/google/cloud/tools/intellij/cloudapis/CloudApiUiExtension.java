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

// TODO: move to core cloud API once dependency is inverted.
public interface CloudApiUiExtension {
  ExtensionPointName<CloudApiUiExtension> EP_NAME =
      new ExtensionPointName<>("com.google.gct.cloudapis.cloudApiUiExtension");

  void init(@NotNull CloudApiUiPresenter uiPresenter);

  void onCurrentCloudLibrarySelected(CloudLibrary currentCloudLibrary, String currentBomVersion);

  void onModuleSelected(Module module);
}
