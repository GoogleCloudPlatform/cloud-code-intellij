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
import com.intellij.openapi.progress.util.ProgressWindow;

class ManagedCloudSdkProgressListener implements ProgressListener {

  private ProgressWindow progressIndicator;
  private double totalWork;
  private double lastUpdatedGlobalDoneFraction;

  @Override
  public void start(String message, long totalWork) {
    this.totalWork = totalWork;

    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              System.out.println("start.main: " + totalWork);

              progressIndicator = new ProgressWindow(true, null);
              progressIndicator.start();

              setProgressText(message);

              if (totalWork == UNKNOWN) {
                progressIndicator.setIndeterminate(true);
              }
            });
  }

  @Override
  public void update(long workDone) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              System.out.println("update.main: " + workDone);
              if (workDone == ProgressListener.UNKNOWN) {
                progressIndicator.setIndeterminate(true);
              } else {
                progressIndicator.setIndeterminate(false);
                lastUpdatedGlobalDoneFraction = workDone / totalWork;
                progressIndicator.setFraction(lastUpdatedGlobalDoneFraction);
                System.out.println("main fraction: " + lastUpdatedGlobalDoneFraction);
              }
            });
  }

  @Override
  public void update(String message) {
    setProgressText(message);
  }

  @Override
  public void done() {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              progressIndicator.stop();
              progressIndicator.dispose();
              System.out.println("done.");
            });
  }

  @Override
  public ProgressListener newChild(long allocation) {
    System.out.println("child.allocation: " + allocation);
    return new ChildProgressListener(allocation);
  }

  private void setProgressText(String message) {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              progressIndicator.setText(GctBundle.message("managedsdk.progress.message", message));
            });
  }

  /** Progress listener for child activities, updates the same progress indicator. */
  private final class ChildProgressListener implements ProgressListener {

    private double childTotalWork;
    private final double globalProgressAllocation;

    private ChildProgressListener(double globalProgressAllocation) {
      this.globalProgressAllocation = globalProgressAllocation;
    }

    @Override
    public void start(String message, long totalWork) {
      System.out.println("start.child: " + totalWork + ", allocated: " + globalProgressAllocation);
      childTotalWork = totalWork;

      setProgressText(message);

      ApplicationManager.getApplication()
          .invokeLater(
              () -> {
                if (totalWork == UNKNOWN) {
                  progressIndicator.setIndeterminate(true);
                }
              });
    }

    @Override
    public void update(long workDone) {
      ApplicationManager.getApplication()
          .invokeLater(
              () -> {
                System.out.println(
                    "update.child: " + workDone + ", allocated: " + globalProgressAllocation);

                if (workDone != ProgressListener.UNKNOWN) {
                  double globalWorkRatio = globalProgressAllocation / totalWork;
                  double childWorkFraction = workDone / childTotalWork;
                  double globalWorkFraction = childWorkFraction * globalWorkRatio;
                  System.out.println("global ratio: " + globalWorkRatio);
                  System.out.println("child work done %: " + childWorkFraction);
                  System.out.println("added to global work %: " + globalWorkFraction);
                  lastUpdatedGlobalDoneFraction += globalWorkFraction;
                  progressIndicator.setFraction(lastUpdatedGlobalDoneFraction);
                  System.out.println("global % done: " + lastUpdatedGlobalDoneFraction);
                } else {
                  // pass unknown state to parent listener.
                  ManagedCloudSdkProgressListener.this.update(UNKNOWN);
                }
              });
    }

    @Override
    public void update(String message) {
      setProgressText(message);
    }

    @Override
    public void done() {
      /* doesn't stop main progress for child progress listeners. */
    }

    @Override
    public ProgressListener newChild(long childAllocation) {
      return new ChildProgressListener(childAllocation);
    }
  }
}
