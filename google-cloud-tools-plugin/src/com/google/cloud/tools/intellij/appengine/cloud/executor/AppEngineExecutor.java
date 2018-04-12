/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.executor;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceManager;
import com.google.cloud.tools.intellij.util.ThreadUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.impl.CancellableRunnable;

/** Executor of {@link AppEngineTask}'s. */
public class AppEngineExecutor implements CancellableRunnable {

  private Process process;
  private AppEngineTask task;

  public AppEngineExecutor(AppEngineTask task) {
    this.task = task;
  }

  @Override
  public void run() {
    task.execute(this::setProcess);
  }

  @Override
  public void cancel() {
    // Only destroy the process and signal cancellation if the process hasn't exited
    if (process != null && process.isAlive()) {
      process.destroy();
      task.onCancel();
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void setProcess(Process process) {
    this.process = process;
    ThreadUtil.getInstance()
        .executeInBackground(
            () -> {
              try {
                CloudSdkServiceManager.getInstance().getSdkReadLock().lock();
                process.waitFor();
              } catch (InterruptedException e) {
                // unexpected interruption, nothing can be done.
                Logger.getInstance(AppEngineExecutor.class)
                    .warn("Waiting for gcloud process unexpectedly interrupted", e);
              } finally {
                CloudSdkServiceManager.getInstance().getSdkReadLock().unlock();
              }
            });
  }

  public Process getProcess() {
    return process;
  }
}
