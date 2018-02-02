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
import com.intellij.openapi.components.ServiceManager;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Helper service to check validity of {@link CloudSdkService}. */
public class CloudSdkValidator {

  public static CloudSdkValidator getInstance() {
    return ServiceManager.getService(CloudSdkValidator.class);
  }

  protected Set<CloudSdkValidationResult> validateCloudSdk(Path path) {
    Set<CloudSdkValidationResult> validationResults = new HashSet<>();

    if (path == null) {
      validationResults.add(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
      // If the Cloud SDK is not found, don't bother checking anything else
      return validationResults;
    }

    CloudSdk sdk = buildCloudSdkWithPath(path);
    try {
      sdk.validateCloudSdk();
    } catch (CloudSdkNotFoundException exception) {
      validationResults.add(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND);
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
      validationResults.add(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT);
    }

    return validationResults;
  }
  /** Checks if the default SDK stored path contains a valid Cloud SDK. */
  public Set<CloudSdkValidationResult> validateCloudSdk() {
    return validateCloudSdk(CloudSdkService.getInstance().getSdkHomePath());
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

    if (isMalformedCloudSdkPath(path)) {
      return ImmutableSet.of(CloudSdkValidationResult.MALFORMED_PATH);
    }

    return validateCloudSdk(Paths.get(path));
  }

  /** Checks if the provided path contains a valid Cloud SDK installation. */
  public boolean isValidCloudSdk(String path) {
    return validateCloudSdk(path).isEmpty();
  }

  /** Checks if the default SDK saved path contains a valid Cloud SDK installation. */
  public boolean isValidCloudSdk() {
    return validateCloudSdk(CloudSdkService.getInstance().getSdkHomePath()).isEmpty();
  }

  /** Checks for invalid characters that trigger an {@link InvalidPathException} on Windows. */
  static boolean isMalformedCloudSdkPath(@Nullable String sdkPath) {
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
