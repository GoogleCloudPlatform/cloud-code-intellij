/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.util;

import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for App Engine.
 */
public class AppEngineUtils {

  /**
   * Returns {@code true} if the provide module is an App Engine module, and returns {@code false} if the module is
   * null or not an App Engine module.
   */
  public static boolean isAppEngineModule(@Nullable Module module) {
    if (module == null) {
      return false;
    }
    return AppEngineGradleFacet.getAppEngineFacetByModule(module) != null;
  }
}
