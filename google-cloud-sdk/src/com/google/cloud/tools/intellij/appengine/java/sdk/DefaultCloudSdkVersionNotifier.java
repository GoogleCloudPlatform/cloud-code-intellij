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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;

/** Service implementation of {@link CloudSdkVersionNotifier} */
public class DefaultCloudSdkVersionNotifier extends CloudSdkVersionNotifier {

  @Override
  public void notifyIfVersionOutOfDate() {
    CloudSdkValidator sdkValidator = CloudSdkValidator.getInstance();

    if (sdkValidator
        .validateCloudSdk()
        .contains(CloudSdkValidationResult.CLOUD_SDK_NOT_MINIMUM_VERSION)) {
      String message =
          "<p>" + CloudSdkValidationResult.CLOUD_SDK_NOT_MINIMUM_VERSION.getMessage() + "</p>";

      showNotification(
          CloudSdkMessageBundle.message("appengine.cloudsdk.version.support.title"), message);
    }
  }

  @Override
  public void notifyIfVersionParseError() {
    CloudSdkValidator sdkValidator = CloudSdkValidator.getInstance();

    if (sdkValidator
        .validateCloudSdk()
        .contains(CloudSdkValidationResult.CLOUD_SDK_VERSION_FILE_ERROR)) {
      String message =
          "<p>" + CloudSdkValidationResult.CLOUD_SDK_VERSION_FILE_ERROR.getMessage() + "</p>";

      showNotification(
          CloudSdkMessageBundle.message("appengine.cloudsdk.version.file.error.title"), message);
    }
  }

  @VisibleForTesting
  void showNotification(String title, String message) {
    NotificationGroup notification =
        new NotificationGroup(title, NotificationDisplayType.BALLOON, true);

    notification
        .createNotification(
            title, message, NotificationType.WARNING, null /* notificationListener */)
        .notify(null /*project*/);
  }
}
