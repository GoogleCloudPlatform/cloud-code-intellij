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

import com.google.gct.idea.util.SystemEnvironmentProvider;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
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
   * Finds the path to the Cloud SDK binary on the local file system.
   *
   * @return a {@link String} path to the Cloud SDK binary or {@code null}
   * if it could not be found.
   */
  @Nullable
  public static String findCloudSdkExecutablePath(
      @NotNull SystemEnvironmentProvider environmentProvider) {
    File cloudSdkExecutable = findCloudSdkExecutable(environmentProvider);
    return cloudSdkExecutable != null ? cloudSdkExecutable.getAbsolutePath() : null;
  }

  /**
   * Finds the path to the Cloud SDK binary parent directory on the local file system.
   *
   * @return a {@link String} path to the Cloud SDK directory or {@code null}
   * if it could not be found.
   */
  @Nullable
  public static String findCloudSdkDirectoryPath(
      @NotNull SystemEnvironmentProvider environmentProvider) {
    File cloudSdkExecutable = findCloudSdkExecutable(environmentProvider);
    return cloudSdkExecutable != null ? cloudSdkExecutable.getParent() : null;
  }

  /**
   * Checks if an appropriately named binary exists on the local file system for the given
   * parent directory path.
   *
   * @param path @link String} to Cloud SDK binary parent directory on local file system.
   * @return a boolean indicating if the file was found.
   */
  public static boolean containsCloudSdkExecutable(String path) {
    return isCloudSdkExecutable(toExecutablePath(path));
  }

  /**
   * Checks if an appropriately named binary exists on the local file system at the given path.
   *
   * @param path @link String} to Cloud SDK binary on local file system.
   * @return a boolean indicating if the file was found.
   */
  public static boolean isCloudSdkExecutable(String path) {
    if (path != null) {
      VirtualFile vfile = LocalFileSystem.getInstance().findFileByPath(path);
      return vfile != null && vfile.exists();
    }

    return false;
  }

  /**
   * Converts from a parent directory path to the Cloud SDK executable path
   *
   */
  public static String toExecutablePath(String sdkDirectoryPath) {
    if (sdkDirectoryPath != null) {
      File executablePath = new File(sdkDirectoryPath, getSystemCommand());
      return executablePath.getAbsolutePath();
    }

    return null;
  }

  /**
   * Converts from a Cloud SDK executable path to its parent directory path
   *
   */
  public static String toParentDirectory(String sdkExecutablePath) {
    if (sdkExecutablePath != null) {
      return new File(sdkExecutablePath).getParent();
    }

    return null;
  }

  private static File findCloudSdkExecutable(
      @NotNull SystemEnvironmentProvider environmentProvider) {
    File gcloudPath = environmentProvider.findInPath(getSystemCommand());
    if (gcloudPath != null) {
      return gcloudPath;
    }
    return null;
  }

  private static String getSystemCommand() {
    return SystemInfo.isWindows ? WIN_COMMAND : UNIX_COMMAND;
  }
}
