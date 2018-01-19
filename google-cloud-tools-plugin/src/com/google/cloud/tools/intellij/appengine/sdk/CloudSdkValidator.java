/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Helper class to check validity of {@link CloudSdkService}. */
public class CloudSdkValidator {

  private final CloudSdkService cloudSdkService;

  public static CloudSdkValidator createFor(CloudSdkService cloudSdkService) {
    return new CloudSdkValidator(cloudSdkService);
  }

  @VisibleForTesting
  CloudSdkValidator(CloudSdkService cloudSdkService) {
    this.cloudSdkService = cloudSdkService;
  }

  protected Set<com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult>
      validateCloudSdk(Path path) {
    Set<com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult> validationResults =
        new HashSet<>();

    if (path == null) {
      validationResults.add(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult
              .CLOUD_SDK_NOT_FOUND);
      // If the Cloud SDK is not found, don't bother checking anything else
      return validationResults;
    }

    CloudSdk sdk = buildCloudSdkWithPath(path);
    try {
      sdk.validateCloudSdk();
    } catch (CloudSdkNotFoundException exception) {
      validationResults.add(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult
              .CLOUD_SDK_NOT_FOUND);
      // If the Cloud SDK is not found, don't bother checking anything else
      return validationResults;
    } catch (CloudSdkOutOfDateException exception) {
      validationResults.add(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult
              .CLOUD_SDK_VERSION_NOT_SUPPORTED);
    }

    try {
      sdk.validateAppEngineJavaComponents();
    } catch (AppEngineJavaComponentsNotInstalledException ex) {
      validationResults.add(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult
              .NO_APP_ENGINE_COMPONENT);
    }

    return validationResults;
  }
  /** Checks if the stored path contains a valid Cloud SDK. */
  public Set<com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult>
      validateCloudSdk() {
    return validateCloudSdk(cloudSdkService.getSdkHomePath());
  }

  /**
   * Checks if a given path is malformed or if it contains a valid Cloud SDK.
   *
   * <p>Windows' implementation of Paths doesn't handle well converting strings with certain special
   * characters to paths. This method should be called before {@code Paths.get(path)}.
   */
  public Set<com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult>
      validateCloudSdk(String path) {
    if (path == null) {
      return ImmutableSet.of(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult
              .CLOUD_SDK_NOT_FOUND);
    }

    if (isMalformedCloudSdkPath(path)) {
      return ImmutableSet.of(
          com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult.MALFORMED_PATH);
    }

    return validateCloudSdk(Paths.get(path));
  }

  /** Checks if the provided path contains a valid Cloud SDK installation. */
  public boolean isValidCloudSdk(String path) {
    return validateCloudSdk(path).isEmpty();
  }

  /** Checks if the saved path contains a valid Cloud SDK installation. */
  public boolean isValidCloudSdk() {
    return validateCloudSdk(cloudSdkService.getSdkHomePath()).isEmpty();
  }

  /** Checks for invalid characters that trigger an {@link InvalidPathException} on Windows. */
  public boolean isMalformedCloudSdkPath(@Nullable String sdkPath) {
    if (sdkPath != null) {
      try {
        Paths.get(sdkPath);
      } catch (InvalidPathException ipe) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  CloudSdk buildCloudSdkWithPath(@NotNull Path path) {
    return new CloudSdk.Builder().sdkPath(path).build();
  }
}
