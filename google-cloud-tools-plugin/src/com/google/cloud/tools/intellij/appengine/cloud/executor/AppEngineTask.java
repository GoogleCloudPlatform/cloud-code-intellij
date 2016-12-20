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

import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.cloud.executor.AppEngineExecutor;

/**
 * Encapsulates a task to be run on Google App Engine.
 */
public abstract class AppEngineTask {

  /**
   * Executes an App Engine task.
   * @param startListener a callback for retrieving the running process
   */
  abstract void execute(ProcessStartListener startListener);

  /**
   * Gets invoked when the task gets cancelled by {@link AppEngineExecutor}.
   * Intentionally left empty - it is up to implementors to decide if they need extra
   * action on cancellation.
   */
  void onCancel() {
  }
}
