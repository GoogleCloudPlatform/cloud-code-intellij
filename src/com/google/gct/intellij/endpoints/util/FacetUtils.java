/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.google.gct.intellij.endpoints.util;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.facet.AndroidFacet;

/**
 * Utilities for working with facets
 * TODO : This class maybe not be useful anymore, maybe move the file finding into something else
 */
public class FacetUtils {

  // don't instantiate
  private FacetUtils() {
  }

  /**
   * Note : requires readAction if not on the dispatch thread
   * Find a file under all the defined content roots for a module
   * @param m
   * @param relPath
   * @return
   */
  public static VirtualFile findFileUnderContentRoots(Module m, String relPath) {
    for (VirtualFile contentRoot : ModuleRootManager.getInstance(m).getContentRoots()) {
      VirtualFile potentialMatch = contentRoot.findFileByRelativePath(relPath);
      if (potentialMatch != null) {
        return potentialMatch;
      }
    }

    return null;
  }
}
