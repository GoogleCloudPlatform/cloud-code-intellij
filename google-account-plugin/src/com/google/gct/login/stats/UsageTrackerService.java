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

import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Usage Tracker Service for obtaining extensions declared in plugin.xml with {@link
 * UsageTrackerExtensionPointBean}
 */
public final class UsageTrackerService {

  private static final KeyedExtensionCollector<UsageTracker, String> COLLECTOR =
      new KeyedExtensionCollector<UsageTracker, String>(
          UsageTrackerExtensionPointBean.EP_NAME.getName());
  private static final UsageTrackerManager usageTrackerManager = UsageTrackerManager.getInstance();

  /**
   * When using the usage tracker, do NOT include any information that can identify the user
   *
   * @return the usage tracker for the current platform
   */
  @NotNull
  public static UsageTracker getInstance() {
    String key = PlatformUtils.getPlatformPrefix();
    return getInstance(key);
  }

  /**
   * For usage tracking in the Cloud Tools family of plugins, we only want one tracker pinging an
   * analytics backend for a given platform. New platform specific trackers can register with their
   * platform prefix key and the UsageTrackerService will select them.  Otherwise, the no-op usage
   * tracker will be used.
   *
   * @param key A string search key associated with an extension
   * @return the first implementation of UsageTracker associated with {@param key}
   */
  static UsageTracker getInstance(@Nullable String key) {
    UsageTracker instance = (key == null) ? null : COLLECTOR.findSingle(key);
    if (instance != null) {
      return instance;
    }
    return new NoOpUsageTracker();
  }

  // Interface for UsageTracker implementations defined in other plugins
  public interface UsageTracker {

    /**
     * When tracking events, do NOT include any information that can identify the user
     */
    void trackEvent(@NotNull String eventCategory,
        @NotNull String eventAction,
        @Nullable String eventLabel,
        @Nullable Integer eventValue);
  }

  // Default no-op implementation of the UsageTracker
  public static class NoOpUsageTracker implements UsageTracker {

    @Override
    public void trackEvent(@NotNull String eventCategory, @NotNull String eventAction,
        @Nullable String eventLabel,
        @Nullable Integer eventValue) {
      // Do nothing
    }
  }

}
