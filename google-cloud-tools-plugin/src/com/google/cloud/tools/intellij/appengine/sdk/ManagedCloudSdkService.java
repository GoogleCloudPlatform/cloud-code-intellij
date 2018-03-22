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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.google.cloud.tools.managedcloudsdk.ConsoleListener;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVerificationException;
import com.google.cloud.tools.managedcloudsdk.ManagedSdkVersionMismatchException;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
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
import java.util.concurrent.CancellationException;
import org.jetbrains.annotations.Nullable;

/**
 * {@link CloudSdkService} providing SDK that is managed and automatically installable, see {@link
 * com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk}.
 *
 * <p>Some methods ({@link #install()}, {@link #update()} ()}) do their work on background thread
 * but listeners methods are always called on EDT thread. This class is not thread-safe, must be
 * used on UI thread.
 */
public class ManagedCloudSdkService implements CloudSdkService {
  private Logger logger = Logger.getInstance(ManagedCloudSdkService.class);

  private ManagedCloudSdk managedCloudSdk;

  private SdkStatus sdkStatus = SdkStatus.NOT_AVAILABLE;

  private final Collection<SdkStatusUpdateListener> statusUpdateListeners = Lists.newArrayList();

  private volatile ListenableFuture<Path> managedSdkBackgroundJob;

  private ProgressListener progressListener;

  @Override
  public void activate() {
    // TODO track event that custom SDK is activated and used.

    initManagedSdk();
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
  public SdkStatus getStatus() {
    return sdkStatus;
  }

  @Override
  public boolean supportsInstall() {
    return managedCloudSdk != null;
  }

  @Override
  public void install() {
    executeManagedSdkJob(ManagedSdkJobType.INSTALL, this::installManagedSdk);
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
    if (ApplicationManager.getApplication().isDispatchThread()) {
      runnable.run();
    } else {
      ApplicationManager.getApplication().invokeLater(runnable);
    }
  }

  /** Creates managed SDK, installs if necessary, and checks for fatal errors. */
  @VisibleForTesting
  void initManagedSdk() {
    try {
      managedCloudSdk = createManagedSdk();
      install();
    } catch (UnsupportedOsException ex) {
      logger.warn("Unsupported OS for Managed Cloud SDK", ex);
      updateStatus(SdkStatus.NOT_AVAILABLE);
      ManagedCloudSdkServiceUiPresenter.getInstance()
          .notifyManagedSdkJobFailure(
              ManagedSdkJobType.INSTALL, GctBundle.message("managedsdk.unsupported.os"));
    }
  }

  /**
   * Runs managed SDK install/update code on background job and handles success/errors.
   *
   * @param jobType Install/update
   * @param managedSdkTask Task to execute.
   * @return {@code true} if task started, {@code false} if Managed SDK is not supported at all.
   */
  private boolean executeManagedSdkJob(ManagedSdkJobType jobType, Callable<Path> managedSdkTask) {
    if (managedCloudSdk == null) {
      return false;
    }

    if (managedSdkBackgroundJob == null || managedSdkBackgroundJob.isDone()) {
      updateStatus(SdkStatus.INSTALLING);
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
    Path installedManagedSdkPath = installSdk();
    installAppEngineJavaComponent();

    return installedManagedSdkPath;
  }

  /** Installs core managed SDK if needed and returns its path if successful. */
  private Path installSdk() throws Exception {
    if (!safeCheckSdkStatus(() -> managedCloudSdk.isInstalled())) {
      ConsoleListener sdkConsoleListener = logger::debug;
      progressListener =
          ManagedCloudSdkServiceUiPresenter.getInstance().createProgressListener(this);

      return managedCloudSdk.newInstaller().install(progressListener, sdkConsoleListener);
    }

    return managedCloudSdk.getSdkHome();
  }

  private void installAppEngineJavaComponent() throws Exception {
    if (!safeCheckSdkStatus(() -> managedCloudSdk.hasComponent(SdkComponent.APP_ENGINE_JAVA))) {
      ConsoleListener appEngineConsoleListener = logger::debug;

      progressListener =
          ManagedCloudSdkServiceUiPresenter.getInstance().createProgressListener(this);
      managedCloudSdk
          .newComponentInstaller()
          .installComponent(
              SdkComponent.APP_ENGINE_JAVA, progressListener, appEngineConsoleListener);
    }
  }

  private Path updateManagedSdk() throws Exception {
    if (!safeCheckSdkStatus(() -> managedCloudSdk.isUpToDate())) {
      ConsoleListener sdkUpdateListener = logger::debug;
      progressListener =
          ManagedCloudSdkServiceUiPresenter.getInstance().createProgressListener(this);

      managedCloudSdk.newUpdater().update(progressListener, sdkUpdateListener);
    }

    return managedCloudSdk.getSdkHome();
  }

  /**
   * Checks SDK status (installed, up-to-date, has component), catching non-fatal exceptions which
   * does not mean SDK cannot be updated or re-installed. Other exceptions are propagated to the
   * caller.
   *
   * @param statusCallable Status check code, returns SDK status result.
   * @return Status of SDK check, false if non-fatal exception was caught.
   */
  private boolean safeCheckSdkStatus(Callable<Boolean> statusCallable) throws Exception {
    try {
      return statusCallable.call();
    } catch (ManagedSdkVerificationException | ManagedSdkVersionMismatchException ex) {
      logger.warn("Unable to check status of existing Managed Cloud SDK, will re-install", ex);
    }
    return false;
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

  enum ManagedSdkJobType {
    INSTALL,
    UPDATE
  }

  /**
   * Managed SDK Job future listener, handles success/error logic, logs errors, shows notifications
   * to a user, updates SDK service statuses.
   */
  private final class ManagedSdkJobListener implements FutureCallback<Path> {
    private final ManagedSdkJobType jobType;

    private ManagedSdkJobListener(ManagedSdkJobType jobType) {
      this.jobType = jobType;
    }

    @Override
    public void onSuccess(Path path) {
      logger.info("Managed Google Cloud SDK successfully installed/updated at: " + path);

      updateStatus(SdkStatus.READY);

      ManagedCloudSdkServiceUiPresenter.getInstance().notifyManagedSdkJobSuccess(jobType);
    }

    @Override
    public void onFailure(Throwable t) {
      if (t instanceof InterruptedException || t instanceof CancellationException) {
        logger.info("Managed Google Cloud SDK install/update cancelled.");

        ManagedCloudSdkServiceUiPresenter.getInstance().notifyManagedSdkJobCancellation(jobType);
      } else {
        logger.warn("Error while installing/updating managed Cloud SDK", t);

        ManagedCloudSdkServiceUiPresenter.getInstance()
            .notifyManagedSdkJobFailure(jobType, t.toString());
      }

      // failed or interrupted update might still keep SDK itself installed.
      switch (jobType) {
        case INSTALL:
          updateStatus(SdkStatus.NOT_AVAILABLE);
          break;
        case UPDATE:
          checkSdkStatusAfterFailedUpdate();
          break;
      }

      // in case of failure progress done() is not called, call explicitly to remove UI.
      progressListener.done();
    }
  }

  private void checkSdkStatusAfterFailedUpdate() {
    try {
      if (managedCloudSdk.isInstalled()) {
        updateStatus(SdkStatus.READY);
      } else {
        updateStatus(SdkStatus.NOT_AVAILABLE);
      }
    } catch (Exception ex) {
      updateStatus(SdkStatus.NOT_AVAILABLE);
    }
  }
}
