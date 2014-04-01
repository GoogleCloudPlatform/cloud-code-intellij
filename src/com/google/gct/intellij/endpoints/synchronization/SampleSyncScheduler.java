/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.intellij.endpoints.synchronization;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules the synchronization of sample templates in a hidden repo every X minutes.
 */
public class SampleSyncScheduler {
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private static SampleSyncScheduler instance;
  private ScheduledFuture<?> taskHandle;

  // ToDo: This delay is being discussed and might change
  private final long DELAY_IN_MINUTES = 120;

  public static SampleSyncScheduler getInstance() {
    if(instance == null) {
      instance = new SampleSyncScheduler();
    }

    return instance;
  }

  public void startScheduleTask() {
    if (taskHandle == null) {
      taskHandle =
        scheduler.scheduleAtFixedRate(SampleSyncTask.getInstance(), 0, DELAY_IN_MINUTES, TimeUnit.MINUTES);
    }
  }
}
