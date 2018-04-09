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

import com.google.cloud.tools.intellij.GoogleCloudCoreIcons;
import com.google.cloud.tools.intellij.appengine.sdk.ManagedCloudSdkService.ManagedSdkJobResult;
import com.google.cloud.tools.intellij.appengine.sdk.ManagedCloudSdkService.ManagedSdkJobType;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for {@link ManagedCloudSdkService}, provides UI notifications and progress updates.
 */
public class ManagedCloudSdkServiceUiPresenter {
  private static ManagedCloudSdkServiceUiPresenter instance =
      new ManagedCloudSdkServiceUiPresenter();

  private static final NotificationGroup NOTIFICATION_GROUP =
      new NotificationGroup(
          new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
          NotificationDisplayType.BALLOON,
          true,
          null,
          GoogleCloudCoreIcons.CLOUD);

  static ManagedCloudSdkServiceUiPresenter getInstance() {
    return instance;
  }

  @VisibleForTesting
  static void setInstance(ManagedCloudSdkServiceUiPresenter uiPresenter) {
    ManagedCloudSdkServiceUiPresenter.instance = uiPresenter;
  }

  public void notifyManagedSdkJobSuccess(ManagedSdkJobType jobType, ManagedSdkJobResult jobResult) {
    String message = GctBundle.message("managedsdk.success." + jobType.name().toLowerCase());
    switch (jobResult) {
      case PROCESSED:
        showNotification(message, NotificationType.INFORMATION);
        return;
      default:
        // do nothing, everything is up-to-date.
    }
  }

  public void notifyManagedSdkJobFailure(ManagedSdkJobType jobType, String errorMessage) {
    String message =
        GctBundle.message("managedsdk.failure." + jobType.name().toLowerCase(), errorMessage);
    showNotification(message, NotificationType.ERROR);
  }

  public void notifyManagedSdkJobCancellation(ManagedSdkJobType jobType) {
    String message = GctBundle.message("managedsdk.cancel." + jobType.name().toLowerCase());
    showNotification(message, NotificationType.WARNING);
  }

  /**
   * Shows notification when Cloud SDK update is about to be started.
   *
   * @param cancelListener Callback if user cancels this update.
   * @param disableListener Callback if user cancels and disables automatic updates.
   * @return Notification.
   */
  public Notification notifyManagedSdkUpdate(
      @NotNull ActionListener cancelListener, @NotNull ActionListener disableListener) {
    Notification notification =
        showNotification(
            GctBundle.message("managedsdk.update.notification"), NotificationType.INFORMATION);
    notification.addAction(
        new AnAction(GctBundle.message("managedsdk.update.notification.cancel")) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            cancelListener.actionPerformed(new ActionEvent(notification, 0, ""));
            notification.expire();
          }
        });
    notification.addAction(
        new AnAction(GctBundle.message("managedsdk.update.notification.disable")) {
          @Override
          public void actionPerformed(AnActionEvent e) {
            disableListener.actionPerformed(new ActionEvent(notification, 0, ""));
            notification.expire();
          }
        });

    return notification;
  }

  public ProgressListener createProgressListener(ManagedCloudSdkService managedCloudSdkService) {
    return new ManagedCloudSdkProgressListener(managedCloudSdkService);
  }

  private Notification showNotification(String message, NotificationType notificationType) {
    Notification notification =
        NOTIFICATION_GROUP.createNotification(
            GctBundle.message("managedsdk.notifications.title"),
            null /*subtitle*/,
            message,
            notificationType);
    notification.notify(null);

    return notification;
  }
}
