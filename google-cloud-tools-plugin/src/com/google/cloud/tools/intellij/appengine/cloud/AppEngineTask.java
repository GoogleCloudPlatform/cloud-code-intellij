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

/**
 * Encapsulates a task to be run on Google App Engine.
 */
public interface AppEngineTask {

  /**
   * Executes an App Engine task.
   * @param startListener a callback for retrieving the running process
   */
  void execute(ProcessStartListener startListener);

}
