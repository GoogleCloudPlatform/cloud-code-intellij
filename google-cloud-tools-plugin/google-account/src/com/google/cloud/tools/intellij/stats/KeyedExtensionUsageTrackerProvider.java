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

import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.util.PlatformUtils;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link UsageTrackerProvider} for obtaining {@link UsageTracker} implementations
 * declared in plugin.xml with {@link UsageTrackerExtensionPointBean}.
 */
public final class KeyedExtensionUsageTrackerProvider extends UsageTrackerProvider {
  private static final KeyedExtensionCollector<UsageTracker, String> COLLECTOR =
      new KeyedExtensionCollector<>(UsageTrackerExtensionPointBean.EP_NAME.getName());

  /**
   * When using the usage tracker, do NOT include any information that can identify the user
   * @return the usage tracker for the current platform.
   */
  @NotNull
  @Override
  protected UsageTracker getTracker() {
    String key = PlatformUtils.getPlatformPrefix();
    return getTracker(key);
  }

  /**
   * For usage tracking in the Cloud Tools family of plugins, we only want one tracker pinging an
   * analytics backend for a given platform. New platform specific trackers can register with their
   * platform prefix key and the UsageTrackerProvider will select them.  Otherwise, the no-op usage
   * tracker will be used.
   *
   * @param key A string search key associated with an extension
   * @return the first implementation of UsageTracker associated with {@param key}
   */
  static UsageTracker getTracker(@Nullable String key) {
    UsageTracker instance = (key == null) ? null : COLLECTOR.findSingle(key);
    if (instance != null) {
      return instance;
    }

    return new NoOpUsageTracker();

  }

  private static class NoOpUsageTracker implements UsageTracker, SendsEvents {

    @Override
    public void sendEvent(@NotNull String eventCategory, @NotNull String eventAction,
        @Nullable Map<String, String> metadataMap) {
      // Do nothing
    }

    @Override
    public FluentTrackingEventWithMetadata trackEvent(String action) {
      return new TrackingEventBuilder(this, "no-category", action);
    }
  }
}
