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

import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.MessageListener;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import com.google.cloud.tools.managedcloudsdk.components.SdkComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.jetbrains.annotations.Nullable;

/**
 * {@link CloudSdkService} providing SDK that is managed and automatically installable, see {@link
 * com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk}.
 *
 * <p>Some methods ({@link #install()} do their work on background thread but listeners methods are
 * always called on EDT thread. This class is not thread-safe, must be used on UI thread.
 */
public class ManagedCloudSdkService implements CloudSdkService {
  private Logger logger = Logger.getInstance(ManagedCloudSdkService.class);

  private ManagedCloudSdk managedCloudSdk;

  private SdkStatus sdkStatus = SdkStatus.NOT_AVAILABLE;

  private final Collection<SdkStatusUpdateListener> statusUpdateListeners = Lists.newArrayList();

  private volatile ListenableFuture<Path> managedSdkBackgroundJob;

  @Override
  public void activate() {
    // TODO track event that custom SDK is activated and used.

    initManagedSdk();
    install();
  }

  @Nullable
  @Override
  public Path getSdkHomePath() {
    switch (sdkStatus) {
      case READY:
        return managedCloudSdk.getSdkHome();
      default:
        return null;
    }
  }

  @Override
  public void setSdkHomePath(String path) {
    /* unsupported, to be removed. */
  }

  @Override
  public SdkStatus getStatus() {
    return sdkStatus;
  }

  @Override
  public boolean install() {
    return executeManagedSdkJob(ManagedSdkJobType.INSTALL, this::installManagedSdk);
  }

  public boolean update() {
    return executeManagedSdkJob(ManagedSdkJobType.UPDATE, this::updateManagedSdk);
  }

  @Override
  public void addStatusUpdateListener(SdkStatusUpdateListener listener) {
    statusUpdateListeners.add(listener);
  }

  @Override
  public void removeStatusUpdateListener(SdkStatusUpdateListener listener) {
    statusUpdateListeners.remove(listener);
  }

  public void cancelInstallOrUpdate() {
    if (managedSdkBackgroundJob != null) {
      managedSdkBackgroundJob.cancel(true);
    }
  }

  @VisibleForTesting
  ManagedCloudSdk createManagedSdk() throws UnsupportedOsException {
    return ManagedCloudSdk.newManagedSdk();
  }

  @VisibleForTesting
  void setLogger(Logger logger) {
    this.logger = logger;
  }

  @VisibleForTesting
  // TODO(ivanporty) move into ThreadUtil common test code.
  void invokeOnApplicationUIThread(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable);
  }

  @VisibleForTesting
  void initManagedSdk() {
    if (managedCloudSdk == null) {
      try {
        managedCloudSdk = createManagedSdk();
      } catch (UnsupportedOsException ex) {
        logger.error("Unsupported OS for Managed Cloud SDK", ex);
        updateStatus(SdkStatus.NOT_AVAILABLE);
      }
    }
  }

  private boolean executeManagedSdkJob(ManagedSdkJobType jobType, Callable<Path> managedSdkTask) {
    if (managedCloudSdk == null) {
      return false;
    }

    if (managedSdkBackgroundJob == null || managedSdkBackgroundJob.isDone()) {
      managedSdkBackgroundJob = ThreadUtil.getInstance().executeInBackground(managedSdkTask);
      Futures.addCallback(managedSdkBackgroundJob, new ManagedSdkJobListener(jobType));
    }

    return true;
  }

  /**
   * Checks for Managed Cloud SDK status and creates/installs it if necessary.
   *
   * @return Path of the installed managed SDK, null if error occurred while installing.
   * @throws InterruptedException if Managed Cloud SDK has been interrupted by {@link
   *     #cancelInstallOrUpdate()} ()}
   */
  private Path installManagedSdk() throws Exception {
    updateStatus(SdkStatus.INSTALLING);

    Path installedManagedSdkPath = installSdk();
    installAppEngineJavaComponent();

    updateStatus(SdkStatus.READY);

    return installedManagedSdkPath;
  }

  /** Installs core managed SDK if needed and returns its path if successful. */
  private Path installSdk() throws Exception {
    if (!managedCloudSdk.isInstalled()) {
      MessageListener sdkInstallListener = logger::debug;

      return managedCloudSdk.newInstaller().install(sdkInstallListener);
    }

    return managedCloudSdk.getSdkHome();
  }

  private void installAppEngineJavaComponent() throws Exception {
    if (!managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA)) {
      MessageListener appEngineInstallListener = logger::debug;

      managedCloudSdk
          .newComponentInstaller()
          .installComponent(SdkComponent.APP_ENGINE_JAVA, appEngineInstallListener);
    }
  }

  private Path updateManagedSdk() throws Exception {
    updateStatus(SdkStatus.INSTALLING);

    if (!managedCloudSdk.isUpToDate()) {
      MessageListener sdkUpdateListener = logger::debug;

      managedCloudSdk.newUpdater().update(sdkUpdateListener);
    }

    updateStatus(SdkStatus.READY);

    return managedCloudSdk.getSdkHome();
  }

  private void updateStatus(SdkStatus sdkStatus) {
    // may be called from install job thread, make sure listeners receive update on UI thread.
    invokeOnApplicationUIThread(
        () -> {
          this.sdkStatus = sdkStatus;
          notifyListeners(this, sdkStatus);
        });
  }

  private void notifyListeners(CloudSdkService sdkService, SdkStatus status) {
    statusUpdateListeners.forEach(listener -> listener.onSdkStatusChange(sdkService, status));
  }

  private enum ManagedSdkJobType {
    INSTALL,
    UPDATE
  }

  private final class ManagedSdkJobListener implements FutureCallback<Path> {
    private final ManagedSdkJobType jobType;

    private ManagedSdkJobListener(ManagedSdkJobType jobType) {
      this.jobType = jobType;
    }

    @Override
    public void onSuccess(Path path) {
      logger.info("Managed Google Cloud SDK successfully installed/updated at: " + path);
    }

    @Override
    public void onFailure(Throwable t) {
      if (t instanceof InterruptedException) {
        handleJobCancellation();
      } else {
        logger.error("Error while installing/updating managed Cloud SDK", t);
        updateStatus(SdkStatus.NOT_AVAILABLE);
      }
    }

    private void handleJobCancellation() {
      logger.info("Managed Google Cloud SDK update cancelled.");
      // interrupted install means not available, but interrupted update does not change status.
      switch (jobType) {
        case INSTALL:
          updateStatus(SdkStatus.NOT_AVAILABLE);
          break;
        case UPDATE:
          break;
      }
    }
  }
}
