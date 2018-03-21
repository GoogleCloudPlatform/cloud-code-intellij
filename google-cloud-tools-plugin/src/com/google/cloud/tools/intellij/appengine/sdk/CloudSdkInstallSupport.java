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
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import java.util.concurrent.CountDownLatch;

/**
 * Utility class providing support for blocking / tracking {@link CloudSdkService#install()} process
 * so that dependent deployment processes can be postponed until installation is complete.
 */
public class CloudSdkInstallSupport {
  private static CloudSdkInstallSupport instance;

  public static CloudSdkInstallSupport getInstance() {
    if (instance == null) {
      instance = new CloudSdkInstallSupport();
    }

    return instance;
  }

  public void deployWhenCloudSdkInstalled(
      CancellableRunnable deploymentRunnable,
      LoggingHandler deploymentLog,
      DeploymentOperationCallback callback) {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();

    System.out.println("SDK Status: " + cloudSdkService.getStatus());
    SdkStatus sdkStatus = cloudSdkService.getStatus();
    boolean supportsInPlaceInstall = false;
    if (sdkStatus != SdkStatus.READY) {
      supportsInPlaceInstall = cloudSdkService.install();
    }
    boolean installInProgress = supportsInPlaceInstall || sdkStatus == SdkStatus.INSTALLING;

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
      deploymentLog.print("Waiting for Google Cloud SDK installation to complete...\n");
    } else {
      // no need to wait for install if unsupported or completed.
      installationCompletionLatch.countDown();
    }

    // wait for SDK to be ready and trigger the actual deployment if it properly installs.
    ThreadUtil.getInstance()
        .executeInBackground(
            () -> {
              try {
                while (installationCompletionLatch.getCount() > 0) {
                  // wait interruptibility to check for user cancel each second.
                  installationCompletionLatch.await(1, SECONDS);
                }

                // at this point the installation should be either ready or user cancelled.
                ApplicationManager.getApplication()
                    .invokeLater(
                        () -> {
                          deployAfterSdkCompleted(cloudSdkService, deploymentRunnable, callback);
                        });

              } catch (InterruptedException e) {
                /* valid cancellation exception, no handling needed. */
              } finally {
                // remove the notification listener regardless of waiting outcome.
                cloudSdkService.removeStatusUpdateListener(sdkStatusUpdateListener);
              }
            });
  }

  private void deployAfterSdkCompleted(
      CloudSdkService cloudSdkService,
      CancellableRunnable deploymentRunnable,
      DeploymentOperationCallback callback) {
    // check the status of SDK after install.
    SdkStatus postInstallSdkStatus = cloudSdkService.getStatus();
    System.out.println("SDK Status post-install check: " + postInstallSdkStatus);
    switch (postInstallSdkStatus) {
      case INSTALLING:
        callback.errorOccurred(
            "Google Cloud SDK with App Engine Java needs to be completely installed to perform this action.");
        return;
      case NOT_AVAILABLE:
        callback.errorOccurred(
            "Google Cloud SDK is not available. Please check Settings -> Google -> Cloud SDK.");
        return;
      case INVALID:
        callback.errorOccurred(
            "Google Cloud SDK is invalid. Please check Settings -> Google -> Cloud SDK.");
        return;
      case READY:
        // can continue to deployment.
        deploymentRunnable.run();
        break;
    }
  }
}
