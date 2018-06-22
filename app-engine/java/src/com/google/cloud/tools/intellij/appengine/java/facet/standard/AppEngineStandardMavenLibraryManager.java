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

import com.google.cloud.tools.intellij.appengine.java.project.MavenProjectService;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/** Maven-specific extension of {@link AppEngineStandardLibraryManager}. */
public class AppEngineStandardMavenLibraryManager implements AppEngineStandardLibraryManager {

  /**
   * Returns {@code false} if the module is a maven module, and {@code true} otherwise.
   *
   * <p>Currently, the App Engine standard managed libraries are only supported for native projects.
   */
  @Override
  public boolean isSupported(@NotNull Module module) {
    return !MavenProjectService.getInstance().isMavenModule(module);
  }
}
