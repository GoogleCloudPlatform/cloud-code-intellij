/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.common.collect.ImmutableSet;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * IntelliJ configured service for providing the path to the Cloud SDK.
 */
public abstract class CloudSdkService {

  public static CloudSdkService getInstance() {
    return ServiceManager.getService(CloudSdkService.class);
  }

  @Nullable
  public abstract Path getSdkHomePath();

  public abstract void setSdkHomePath(String path);

  protected abstract Set<CloudSdkValidationResult> validateCloudSdk(Path path);

  /**
   * Checks if the stored path contains a valid Cloud SDK.
   */
  public Set<CloudSdkValidationResult> validateCloudSdk() {
    return validateCloudSdk(getSdkHomePath());
  }

  /**
   * Checks if a given path is malformed or if it contains a valid Cloud SDK.
   *
   * <p>Windows' implementation of Paths doesn't handle well converting strings with certain special
   * characters to paths. This method should be called before {@code Paths.get(path)}.
   */
  public Set<CloudSdkValidationResult> validateCloudSdk(String path) {
    if (path == null) {
      return ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
    }

    try {
      return validateCloudSdk(Paths.get(path));
    } catch (InvalidPathException ipe) {
      return ImmutableSet.of(CloudSdkValidationResult.MALFORMED_PATH);
    }
  }

  /**
   * Checks if the provided path contains a valid Cloud SDK installation.
   */
  public boolean isValidCloudSdk(String path) {
    return validateCloudSdk(path).isEmpty();
  }

  /**
   * Checks if the saved path contains a valid Cloud SDK installation.
   */
  public boolean isValidCloudSdk() {
    return validateCloudSdk(getSdkHomePath()).isEmpty();
  }

  @Nullable
  public abstract File getToolsApiJarFile();

  @NotNull
  public abstract File[] getLibraries();

  @NotNull
  public abstract File[] getJspLibraries();

  public abstract boolean isMethodInBlacklist(
      @NotNull String className, @NotNull String methodName);

  public abstract boolean isClassInWhiteList(@NotNull String className);

  @Nullable
  public abstract File getWebSchemeFile();

  public abstract void patchJavaParametersForDevServer(@NotNull ParametersList vmParameters);

}
