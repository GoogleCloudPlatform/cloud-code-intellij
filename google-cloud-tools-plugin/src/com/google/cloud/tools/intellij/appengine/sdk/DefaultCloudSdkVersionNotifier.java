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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;

/**
 * Service implementation of {@link CloudSdkVersionNotifier}
 */
public class DefaultCloudSdkVersionNotifier extends CloudSdkVersionNotifier {

  @Override
  public void notifyIfUnsupportedVersion(@NotNull Path cloudSdkPath) {
    Set<CloudSdkValidationResult> results = CloudSdkService.getInstance()
        .validateCloudSdk(cloudSdkPath);
    if (results.contains(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED)) {
      showNotification();
    }
  }

  @VisibleForTesting
  void showNotification() {
    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("appengine.cloudsdk.version.support.title"),
            NotificationDisplayType.BALLOON,
            true);

    String message = "<p>"
        + CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED.getMessage()
        + "</p>";

    notification
        .createNotification(
            GctBundle.message("appengine.cloudsdk.version.support.title"),
            message,
            NotificationType.WARNING,
            null /* notificationListener */)
        .notify(null /*project*/);
  }

}
