/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;

import com.intellij.openapi.vcs.impl.CancellableRunnable;

/**
 * Runner of {@link AppEngineTask}'s.
 */
public class AppEngineRunner implements CancellableRunnable {

  private Process process;
  private AppEngineTask task;

  public AppEngineRunner(AppEngineTask task) {
    this.task = task;
  }

  @Override
  public void run() {
    task.execute(new ProcessStartListener() {
      @Override
      public void onStart(Process process) {
        setProcess(process);
      }
    });
  }

  @Override
  public void cancel() {
    if (process != null) {
      process.destroy();
      task.onCancel();
    }
  }

  private void setProcess(Process process) {
    this.process = process;
  }

}
