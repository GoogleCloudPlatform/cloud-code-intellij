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

import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.managedcloudsdk.ProgressListener;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

/**
 * Progress listener for {@link ManagedCloudSdkService} installation and update processes. Uses
 * {@link BackgroundableProcessIndicator} to show progress while running managed SDK jobs. UI
 * provides cancel option.
 *
 * <p>This class is not thread-safe and must be called from single thread (managed SDK job thread).
 */
class ManagedCloudSdkProgressListener implements ProgressListener {

  private final ManagedCloudSdkService managedCloudSdkService;

  private BackgroundableProcessIndicator progressIndicator;
  private Backgroundable task;

  private double totalWork;
  private double lastUpdatedGlobalDoneFraction;

  ManagedCloudSdkProgressListener(ManagedCloudSdkService managedCloudSdkService) {
    this.managedCloudSdkService = managedCloudSdkService;
  }

  @Override
  public void start(String message, long totalWork) {
    this.totalWork = totalWork;

    // initialize progress indicator first in UI thread before using it for updates.
    this.progressIndicator = initProgressIndicator();

    setProgressText(message);

    if (totalWork == UNKNOWN) {
      progressIndicator.setIndeterminate(true);
    }
  }

  @Override
  public void update(long workDone) {
    if (workDone == UNKNOWN) {
      progressIndicator.setIndeterminate(true);
    } else {
      progressIndicator.setIndeterminate(false);
      lastUpdatedGlobalDoneFraction += workDone / totalWork;
      progressIndicator.setFraction(lastUpdatedGlobalDoneFraction);
    }
  }

  @Override
  public void update(String message) {
    setProgressText(message);
  }

  @Override
  public void done() {
    if (!progressIndicator.isFinished(task)) {
      progressIndicator.finish(task);
      progressIndicator.dispose();
    }
  }

  @Override
  public ProgressListener newChild(long allocation) {
    return new ChildProgressListener(allocation);
  }

  private void setProgressText(String message) {
    progressIndicator.setText(GctBundle.message("managedsdk.progress.message", message));
  }

  @VisibleForTesting
  BackgroundableProcessIndicator initProgressIndicator() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // task info for progress indicator, required to init/finish properly.
              task =
                  new Backgroundable(
                      null /* not project specific task */,
                      GctBundle.message("managedsdk.notifications.title"),
                      true /* cancellable */,
                      PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                      // will not be called in manual progress mode.
                    }
                  };
              progressIndicator = new BackgroundableProcessIndicator(task);
              Disposer.register(ApplicationManager.getApplication(), progressIndicator);
              progressIndicator.addStateDelegate(
                  new AbstractProgressIndicatorExBase() {
                    @Override
                    public void cancel() {
                      managedCloudSdkService.cancelInstallOrUpdate();
                      done();
                    }
                  });
            });

    return progressIndicator;
  }

  /**
   * Progress listener for child activities, updates the same progress indicator. Updates progress
   * indicator with proportioned chunk of global progress.
   */
  private final class ChildProgressListener implements ProgressListener {

    private double childWork;
    /* 0 < childTotalWorkAllocation <= totalWork */
    private final double childTotalWorkAllocation;

    private ChildProgressListener(double childTotalWorkAllocation) {
      this.childTotalWorkAllocation = childTotalWorkAllocation;
    }

    @Override
    public void start(String message, long childWork) {
      this.childWork = childWork;

      setProgressText(message);

      if (childWork == UNKNOWN) {
        // pass unknown state to parent listener.
        ManagedCloudSdkProgressListener.this.update(UNKNOWN);
      }
    }

    @Override
    public void update(long childWorkDone) {
      // both could be UNKNOWN meaning undetermined total progress for the whole child task.
      if (childWorkDone != UNKNOWN && childWork != UNKNOWN) {
        double globalWorkRatio = childTotalWorkAllocation / totalWork;
        double childWorkFraction = childWorkDone / childWork;
        double globalWorkFraction = childWorkFraction * globalWorkRatio;
        lastUpdatedGlobalDoneFraction += globalWorkFraction;
        progressIndicator.setFraction(lastUpdatedGlobalDoneFraction);
      } else {
        // pass unknown state to parent listener.
        ManagedCloudSdkProgressListener.this.update(UNKNOWN);
      }
    }

    @Override
    public void update(String message) {
      setProgressText(message);
    }

    @Override
    public void done() {
      /* don't stop main progress for child progress listeners. */
    }

    @Override
    public ProgressListener newChild(long childAllocation) {
      return new ChildProgressListener(childAllocation);
    }
  }
}
