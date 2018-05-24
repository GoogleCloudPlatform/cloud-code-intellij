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

package com.google.cloud.tools.intellij.analytics;

import com.intellij.openapi.application.ApplicationManager;
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
   *
   * @return the usage tracker for the current platform.
   */
  @NotNull
  @Override
  protected UsageTracker getTracker() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return new NoOpUsageTracker();
    }

    String excludedPlatformKey = PlatformUtils.getPlatformPrefix();
    return getTracker(excludedPlatformKey);
  }

  /**
   * For usage tracking in in the Cloud Tools plugin we track usage across all platforms except for
   * those excluded in the usageTracker extensions. To exclude a platform, add a new usageTracker
   * extension with a key equal to the platform key, and with an implementation class of {@link
   * NoOpUsageTracker}. For all those not excluded, the {@link GoogleUsageTracker} will be returned.
   *
   * @param excludedPlatformKey A string search key associated with an extension representing a
   *     platform to exclude
   * @return the first implementation of UsageTracker associated with {@param key}
   */
  static UsageTracker getTracker(@Nullable String excludedPlatformKey) {
    UsageTracker instance =
        (excludedPlatformKey == null) ? null : COLLECTOR.findSingle(excludedPlatformKey);
    if (instance != null) {
      return instance;
    }

    return new GoogleUsageTracker();
  }

  private static class NoOpUsageTracker implements UsageTracker, SendsEvents {

    @Override
    public void sendEvent(
        @NotNull String eventCategory,
        @NotNull String eventAction,
        @Nullable Map<String, String> metadataMap) {
      // Do nothing
    }

    @Override
    public FluentTrackingEventWithMetadata trackEvent(String action) {
      return new TrackingEventBuilder(this, "no-category", action);
    }
  }
}
