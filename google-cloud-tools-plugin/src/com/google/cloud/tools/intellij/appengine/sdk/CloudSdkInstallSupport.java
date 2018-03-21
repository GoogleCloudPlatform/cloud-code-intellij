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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
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

  public SdkStatus waitUntilCloudSdkInstalled(Project project) {
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();

    System.out.println("SDK Status: " + cloudSdkService.getStatus());
    SdkStatus sdkStatus = cloudSdkService.getStatus();
    boolean supportsInPlaceInstall = false;
    if (sdkStatus == SdkStatus.NOT_AVAILABLE) {
      supportsInPlaceInstall = cloudSdkService.install();
    }
    boolean installingInProgress = supportsInPlaceInstall || sdkStatus == SdkStatus.INSTALLING;
    if (installingInProgress) {
      // wait until install / update is done. use latch to notify UI blocking thread.
      CountDownLatch installationCompletionLatch = new CountDownLatch(1);
      final CloudSdkService.SdkStatusUpdateListener sdkStatusUpdateListener =
          (sdkService, status) -> {
            System.out.println("SDK status change to: " + status);
            switch (status) {
              case READY:
              case INVALID:
              case NOT_AVAILABLE:
                System.out.println("latch triggered.");
                installationCompletionLatch.countDown();
                break;
              case INSTALLING:
                // continue waiting for completion.
                break;
            }
          };
      cloudSdkService.addStatusUpdateListener(sdkStatusUpdateListener);

      ProgressManager.getInstance()
          .runProcessWithProgressSynchronously(
              () -> {
                ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

                try {
                  while (installationCompletionLatch.getCount() > 0) {
                    // wait interruptibility to check for user cancel each second.
                    installationCompletionLatch.await(1, SECONDS);
                    if (ProgressManager.getInstance().getProgressIndicator().isCanceled()) {
                      return;
                    }
                  }
                } catch (InterruptedException e) {
                  /* valid cancellation exception, no handlind needed. */
                }
              },
              "Waiting for Google Cloud SDK Installation to Complete...",
              true,
              project);

      cloudSdkService.removeStatusUpdateListener(sdkStatusUpdateListener);
    }

    ApplicationManager.getApplication().invokeAndWait(() -> {});

    System.out.println("cloud service: " + cloudSdkService);
    new Exception().printStackTrace();
    System.out.println("status: " + cloudSdkService.getStatus());

    return cloudSdkService.getStatus();
  }
}
