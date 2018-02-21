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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import org.jetbrains.annotations.NotNull;

/**
 * Progress listener for {@link com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk} installation
 * and update processes. Uses {@link BackgroundableProcessIndicator} to show progress while running
 * managed SDK jobs. UI provides cancel option, passing it to cancel callback.
 */
class ManagedCloudSdkProgressListener implements ProgressListener {

  private final Runnable cancelCallback;
  private BackgroundableProcessIndicator progressIndicator;
  private double totalWork;
  private double lastUpdatedGlobalDoneFraction;
  private Backgroundable task;

  ManagedCloudSdkProgressListener(Runnable cancelCallback) {
    this.cancelCallback = cancelCallback;
  }

  @Override
  public void start(String message, long totalWork) {
    this.totalWork = totalWork;

    // initialize progress indicator first in UI thread before using for updates.
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              // task info for progress indicator, required to init/finish properly.
              task =
                  new Backgroundable(
                      null /* not project specific task */,
                      "" /* set in each message */,
                      true /* cancellable */,
                      PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                      // will not be called in manual progress mode.
                    }
                  };
              progressIndicator = new BackgroundableProcessIndicator(task);
              progressIndicator.addStateDelegate(
                  new AbstractProgressIndicatorExBase() {
                    @Override
                    public void cancel() {
                      cancelCallback.run();
                      done();
                    }
                  });

              setProgressText(message);

              if (totalWork == UNKNOWN) {
                progressIndicator.setIndeterminate(true);
              }
            });
  }

  @Override
  public void update(long workDone) {
    if (workDone == ProgressListener.UNKNOWN) {
      progressIndicator.setIndeterminate(true);
    } else {
      progressIndicator.setIndeterminate(false);
      lastUpdatedGlobalDoneFraction = workDone / totalWork;
      progressIndicator.setFraction(lastUpdatedGlobalDoneFraction);
    }
  }

  @Override
  public void update(String message) {
    setProgressText(message);
  }

  @Override
  public void done() {
    progressIndicator.finish(task);
    progressIndicator.dispose();
  }

  @Override
  public ProgressListener newChild(long allocation) {
    return new ChildProgressListener(allocation);
  }

  private void setProgressText(String message) {
    progressIndicator.setText(GctBundle.message("managedsdk.progress.message", message));
  }

  /**
   * Progress listener for child activities, updates the same progress indicator. Updates progress
   * with proportioned chunk of global progress.
   */
  private final class ChildProgressListener implements ProgressListener {

    private double childTotalWork;
    private final double globalProgressAllocation;

    private ChildProgressListener(double globalProgressAllocation) {
      this.globalProgressAllocation = globalProgressAllocation;
    }

    @Override
    public void start(String message, long totalWork) {
      childTotalWork = totalWork;

      setProgressText(message);

      if (totalWork == UNKNOWN) {
        progressIndicator.setIndeterminate(true);
      }
    }

    @Override
    public void update(long workDone) {
      if (workDone != ProgressListener.UNKNOWN && childTotalWork != UNKNOWN) {
        double globalWorkRatio = globalProgressAllocation / totalWork;
        double childWorkFraction = workDone / childTotalWork;
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
