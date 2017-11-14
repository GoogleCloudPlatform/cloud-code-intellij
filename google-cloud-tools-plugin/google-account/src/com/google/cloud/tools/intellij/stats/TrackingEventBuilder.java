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

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.util.Maps;
import com.google.cloud.tools.intellij.stats.UsageTracker.FluentTrackingEventWithMetadata;
import java.util.Map;

/**
 * Implements the fluent interface exposed for tracking by {@link UsageTracker}.
 */
class TrackingEventBuilder implements FluentTrackingEventWithMetadata {

  private SendsEvents eventSender;
  private String category;
  private String action;
  private Map<String, String> metadataMap = Maps.newHashMap();

  TrackingEventBuilder(SendsEvents eventSender, String category, String action) {
    this.eventSender = Preconditions.checkNotNull(eventSender);
    this.category = Preconditions.checkNotNull(category);
    this.action = Preconditions.checkNotNull(action);
  }

  @Override
  public FluentTrackingEventWithMetadata addMetadata(String key, String value) {
    metadataMap.put(Preconditions.checkNotNull(key), Preconditions.checkNotNull(value));
    return this;
  }

  @Override
  public void ping() {
    eventSender.sendEvent(category, action, metadataMap);
  }
}
