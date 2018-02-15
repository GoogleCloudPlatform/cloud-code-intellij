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
  private long totalWork;

  @Override
  public void start(String message, long totalWork) {
    this.totalWork = totalWork;

    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              progressIndicator = new ProgressWindow(true, null);
              progressIndicator.start();

              setProgressText(message);
            });

    System.out.println("start.main: " + totalWork);
  }

  @Override
  public void update(long workDone) {
    System.out.println("update.main: " + workDone);
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              if (workDone == ProgressListener.UNKNOWN) {
                progressIndicator.setIndeterminate(true);
              } else {
                progressIndicator.setIndeterminate(false);
                progressIndicator.setFraction(workDone / totalWork);
              }
            });
  }

  @Override
  public void update(String message) {
    setProgressText(message);
  }

  @Override
  public void done() {
    System.out.println("done.");
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              progressIndicator.stop();
              progressIndicator.dispose();
            });
  }

  @Override
  public ProgressListener newChild(long allocation) {
    System.out.println("child.allocation: " + allocation);
    return new ChildProgressListener(totalWork, allocation);
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

    private final long totalWork;
    private final long allocation;

    private ChildProgressListener(long totalWork, long allocation) {
      this.totalWork = totalWork;
      this.allocation = allocation;
    }

    @Override
    public void start(String message, long totalWork) {
      System.out.println("start.child: " + totalWork + ", allocated: " + allocation);

      setProgressText(message);
    }

    @Override
    public void update(long workDone) {
      System.out.println("update.child: " + workDone + ", allocated: " + allocation);

      ApplicationManager.getApplication()
          .invokeLater(
              () -> {
                double allocationFraction = (double) workDone * allocation / totalWork;
                progressIndicator.setFraction(allocationFraction);
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
      return new ChildProgressListener(allocation, childAllocation);
    }
  }
}
