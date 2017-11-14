/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stats;

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;

/**
 * Usage Tracker Provider (as a Service) for obtaining UsageTracker implementations.
 */
public abstract class UsageTrackerProvider {

  @NotNull
  public static UsageTracker getInstance() {
    return ServiceManager.getService(UsageTrackerProvider.class).getTracker();
  }

  /**
   * Do not return a tracker that includes PII.
   */
  @NotNull
  protected abstract UsageTracker getTracker();

}
