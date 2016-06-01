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

import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.impl.CancellableRunnable;

/**
 * Runnable that executes task responsible for stopping an App Engine application.
 */
public class AppEngineStopRunner implements CancellableRunnable {
  private static final Logger logger = Logger.getInstance(AppEngineStopRunner.class);

  private AppEngineStop stop;
  private String module;
  private String version;

  private Process process;

  /**
   * Initialize the stop runner's dependencies.
   */
  public AppEngineStopRunner(AppEngineStop stop, String module, String version) {
    this.stop = stop;
    this.module = module;
    this.version = version;
  }

  @Override
  public void run() {
    try {
      stop.stop(module, version, new ProcessStartListener() {
        @Override
        public void start(Process process) {
          setProcess(process);
        }
      });
    } catch (RuntimeException re) {
      stop.getCallback().errorOccurred(GctBundle.message("appengine.stop.modules.version.error"));
      logger.error(re);
    }
  }

  @Override
  public void cancel() {
    if (process != null) {
      process.destroy();
    }
  }

  private void setProcess(Process process) {
    this.process = process;
  }
}
