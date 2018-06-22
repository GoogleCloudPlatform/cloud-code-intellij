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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/** Extension point for managing App Engine standard managed libraries. */
public interface AppEngineStandardLibraryManager {
  ExtensionPointName<AppEngineStandardLibraryManager> EP_NAME =
      ExtensionPointName.create("com.google.gct.core.appEngineStandardLibraryManager");

  /**
   * Returns {@code true} if App Engine managed libraries are supported by the implementing
   * extension, and {@code false} otherwise.
   */
  boolean isSupported(@NotNull Module module);
}
