/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.login.stats;

import com.google.gct.login.stats.UsageTrackerService.UsageTracker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Google Usage Tracker that reports to Cloud Tools Analytics backend
 */
public class GoogleUsageTracker implements UsageTracker {
  @Override
  public void trackEvent(@NotNull String eventCategory,
                         @NotNull String eventAction,
                         @Nullable String eventLabel,
                         @Nullable Integer eventValue) {
    // TODO : report to our analytics backend when all things are in place : TOS, configurable settings, backend.
  }

}
