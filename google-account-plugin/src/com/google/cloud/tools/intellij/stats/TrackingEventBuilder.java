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

package com.google.cloud.tools.intellij.stats;

import com.google.cloud.tools.intellij.stats.UsageTracker.PartialTrackingEventAction;
import com.google.cloud.tools.intellij.stats.UsageTracker.PartialTrackingEventAction.PartialTrackingEventLabel;
import com.google.cloud.tools.intellij.stats.UsageTracker.PartialTrackingEventAction.PartialTrackingEventLabel.PartialTrackingEventValue;
import com.google.cloud.tools.intellij.stats.UsageTracker.SendsEvent;

/**
 * Implements the fluent interface exposed for tracking by {@link UsageTracker}.
 */
class TrackingEventBuilder implements
    PartialTrackingEventAction, PartialTrackingEventLabel, PartialTrackingEventValue {

  private UsageTracker googleUsageTracker;
  private String category;
  private String action;
  private String label;
  private Integer value;

  TrackingEventBuilder(UsageTracker googleUsageTracker, String category) {
    this.googleUsageTracker = googleUsageTracker;
    this.category = category;
  }

  @Override
  public PartialTrackingEventLabel withAction(String action) {
    this.action = action;
    return this;
  }

  @Override
  public PartialTrackingEventValue andLabel(String label) {
    this.label = label;
    return this;
  }

  @Override
  public SendsEvent setValue(Integer value) {
    this.value = value;
    return this;
  }

  @Override
  public void send() {
    googleUsageTracker.trackEvent(category, action, label, value);
  }
}
