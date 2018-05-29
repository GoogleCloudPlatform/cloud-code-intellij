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

package com.google.cloud.tools.intellij.analytics;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A NoOp implementation of {@link UsageTrackerService} and {@link SendsEvents}. */
public class NoOpUsageTrackerService implements UsageTrackerService, SendsEvents {

  @Override
  public FluentTrackingEventWithMetadata trackEvent(String action) {
    return new TrackingEventBuilder(this, "no-category", action);
  }

  @Override
  public void sendEvent(
      @NotNull String eventCategory,
      @NotNull String eventAction,
      @Nullable Map<String, String> metadataMap) {
    // Do nothing
  }
}
