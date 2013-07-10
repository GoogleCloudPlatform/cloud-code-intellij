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

import java.io.File;
import java.io.IOException;

/**
 * Utilities for working with resources and files
 */
public class ResourceUtils {

  private ResourceUtils() {
  }

  /** create an empty file if it doesn't exist already */
  public static void createResource(File f) throws IOException {
    if (f.exists()) {
      return;
    }
    createResource(f.getParentFile());
    if (f.getName().contains(".")) {
      f.createNewFile();
    }
    else {
      f.mkdir();
    }
  }

  /** Delete a file or directory recursively
   * CAREFUL : Doesn't handle symlinks at all */
  public static void deleteResource(File f) throws IOException {
    if (!f.exists()) {
      return;
    }

    if (f.isFile()) {
      f.delete();
      return;
    }

    // We're dealing with a directory
    for (File child : f.listFiles()) {
      deleteResource(child);
    }

    f.delete();
  }
}
