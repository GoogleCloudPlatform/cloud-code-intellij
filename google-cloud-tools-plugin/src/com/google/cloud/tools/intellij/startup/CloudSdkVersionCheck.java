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

package com.google.cloud.tools.intellij.startup;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ProcessRunnerException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;

/**
 * Checks that the configured Cloud SDK's version is supported, and warns the user if the Cloud SDK
 * needs to be updated.
 */
public class CloudSdkVersionCheck {

  private static final Logger logger = Logger.getInstance(CloudSdkVersionCheck.class);

  /**
   * Notify the user that they need to update their Cloud SDK installation, if it is not supported.
   *
   * @return true if a notification was shown
   */
  public boolean notifyIfCloudSdkNotSupported(CloudSdk sdk) {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();
    if (!cloudSdkService.isCloudSdkVersionSupported(sdk)) {

      CloudSdkVersion requiredVersion = cloudSdkService.getMinimumRequiredCloudSdkVersion();

      CloudSdkVersion actualVersion;
      try {
        actualVersion = sdk.getVersion();
      } catch (ProcessRunnerException exception) {
        logger.error("Exception encountered when calling the cloud SDK", exception);
        return false;
      }

      showNotification(actualVersion, requiredVersion);
      return true;
    }
    return false;
  }

  private void showNotification(CloudSdkVersion found, CloudSdkVersion required) {
    NotificationGroup notification =
        new NotificationGroup(
            GctBundle.message("appengine.cloudsdk.version.support.title"),
            NotificationDisplayType.BALLOON,
            true);

    String message = "<p>"
        + GctBundle.message("appengine.cloudsdk.version.support.message",
            found.toString(), required.toString())
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
