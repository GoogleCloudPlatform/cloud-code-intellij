/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.util.SystemInfo;

import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Handy Cloud SDK utility methods.
 *
 * Not for instantiation.
 */
public final class CloudSdkUtil {

  private CloudSdkUtil() {
    // Not designed for instantiation.
  }
  private static final String UNIX_COMMAND = "gcloud";
  private static final String WIN_COMMAND = "gcloud.cmd";

  /**
   * Finds the Cloud SDK path on the local file system.
   *
   * @return a {@link String} path to the Cloud SDK or {@code null} if it could not be found.
   */
  @Nullable
  public static String findCloudSdkPath() {
    File gcloudPath = PathEnvironmentVariableUtil
        .findInPath(SystemInfo.isWindows ? WIN_COMMAND : UNIX_COMMAND);
    if (gcloudPath != null) {
      return gcloudPath.getAbsolutePath();
    }
    return null;
  }
}
