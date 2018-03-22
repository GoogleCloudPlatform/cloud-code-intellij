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

package com.google.cloud.tools.intellij.appengine.sdk;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import java.util.concurrent.CountDownLatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class providing support for blocking / tracking {@link CloudSdkService#install()} process
 * and other SDK preconditions so that dependent deployment processes can be postponed until SDK is
 * completely ready.
 */
public class CloudSdkPreconditionsSupport {
  private static CloudSdkPreconditionsSupport instance;

  public static CloudSdkPreconditionsSupport getInstance() {
    if (instance == null) {
      instance = new CloudSdkPreconditionsSupport();
    }

    return instance;
  }

  /**
   * Waits for Cloud SDK to be ready for all operations and then runs the given runnable. If process
   * results in error or user cancel, shows notification and does not run.
   *
   * @param project Project to which runnable belongs.
   * @param runnable Runnable to run after Cloud SDK is ready.
   * @param deploymentLog Log to print SDK statuses.
   * @param callback Callback to report errors.
   */
  public void runAfterCloudSdkPreconditionsMet(
      @Nullable Project project,
      Runnable runnable,
      LoggingHandler deploymentLog,
      DeploymentOperationCallback callback) {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();

    SdkStatus sdkStatus = cloudSdkService.getStatus();
    boolean installInProgress = sdkStatus == SdkStatus.INSTALLING;
    // if not already installing and still not ready, attempt to fix and install now.
    if (!installInProgress && sdkStatus != SdkStatus.READY && cloudSdkService.supportsInstall()) {
      cloudSdkService.install();
      installInProgress = true;
    }

    CountDownLatch installationCompletionLatch = new CountDownLatch(1);
    // listener for SDK updates, waits until install / update is done. uses latch to notify UI
    // blocking thread.
    final CloudSdkService.SdkStatusUpdateListener sdkStatusUpdateListener =
        (sdkService, status) -> {
          switch (status) {
            case READY:
            case INVALID:
            case NOT_AVAILABLE:
              installationCompletionLatch.countDown();
              break;
            case INSTALLING:
              // continue waiting for completion.
              break;
          }
        };

    if (installInProgress) {
      cloudSdkService.addStatusUpdateListener(sdkStatusUpdateListener);
      deploymentLog.print(GctBundle.getString("appengine.deployment.status.preparing.sdk") + "\n");
    } else {
      // no need to wait for install if unsupported or completed.
      installationCompletionLatch.countDown();
    }

    // wait for SDK to be ready and trigger the actual deployment if it properly installs.
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(
                project, GctBundle.message("appengine.deployment.status.deploying"), true) {
              @Override
              public void run(@NotNull ProgressIndicator indicator) {
                try {
                  while (installationCompletionLatch.getCount() > 0) {
                    // wait interruptibility to check for user cancel each second.
                    installationCompletionLatch.await(1, SECONDS);
                    if (checkIfCancelled()) {
                      break;
                    }
                  }

                  // at this point the installation should be either ready or user cancelled.
                  ApplicationManager.getApplication()
                      .invokeLater(
                          () -> {
                            doRun(cloudSdkService, runnable, callback);
                          });

                } catch (InterruptedException e) {
                  /* valid cancellation exception, no handling needed. */
                } finally {
                  // remove the notification listener regardless of waiting outcome.
                  cloudSdkService.removeStatusUpdateListener(sdkStatusUpdateListener);
                }
              }
            });
  }

  @VisibleForTesting
  boolean checkIfCancelled() {
    return ProgressManager.getInstance().getProgressIndicator().isCanceled();
  }

  private void doRun(
      CloudSdkService cloudSdkService, Runnable runnable, DeploymentOperationCallback callback) {
    // check the status of SDK after install.
    SdkStatus postInstallSdkStatus = cloudSdkService.getStatus();
    switch (postInstallSdkStatus) {
      case INSTALLING:
        String message = GctBundle.message("appengine.deployment.error.sdk.still.installing");
        callback.errorOccurred(message);
        showCloudSdkNotification(
            message, NotificationType.WARNING, false /* no settings needed for this case. */);
        return;
      case NOT_AVAILABLE:
        String errorMessage = GctBundle.message("appengine.deployment.error.sdk.not.available");
        callback.errorOccurred(errorMessage);
        showCloudSdkNotification(errorMessage, NotificationType.ERROR, true);
        return;
      case INVALID:
        errorMessage = GctBundle.message("appengine.deployment.error.sdk.invalid");
        callback.errorOccurred(errorMessage);
        showCloudSdkNotification(errorMessage, NotificationType.ERROR, true);
        return;
      case READY:
        // can continue to deployment.
        runnable.run();
        break;
    }
  }

  @VisibleForTesting
  void showCloudSdkNotification(
      String errorMessage, NotificationType notificationType, boolean showSettingsAction) {
    if (!CloudSdkValidator.getInstance().isValidCloudSdk()) {
      Notification invalidSdkWarning =
          new Notification(
              new PropertiesFileFlagReader().getFlagString("notifications.plugin.groupdisplayid"),
              GctBundle.message("settings.menu.item.cloud.sdk.text"),
              errorMessage,
              notificationType);
      // add a link to SDK settings for a quick fix.
      if (showSettingsAction) {
        invalidSdkWarning.addAction(
            new AnAction(GctBundle.message("appengine.deployment.error.sdk.settings.action")) {
              @Override
              public void actionPerformed(AnActionEvent e) {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, CloudSdkConfigurable.class);
                // expire if action has been called to avoid error hanging out forever.
                invalidSdkWarning.expire();
              }
            });
      }

      Notifications.Bus.notify(invalidSdkWarning);
    }
  }
}
