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

package com.google.cloud.tools.intellij.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for defining the actual tracking behavior, implementations must be declared in
 * plugin.xml for the {@link UsageTrackerExtensionPointBean} extension point.
 */
public interface UsageTracker {

  /**
   * When tracking events, do NOT include any information that can identify the user.
   */
  void trackEvent(@NotNull String eventCategory,
      @NotNull String eventAction,
      @Nullable String eventLabel,
      @Nullable Integer eventValue);

}
