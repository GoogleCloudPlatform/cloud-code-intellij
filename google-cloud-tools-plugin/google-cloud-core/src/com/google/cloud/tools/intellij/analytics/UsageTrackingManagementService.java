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

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.Nullable;

/**
 * This service provides management APIs for the analytics UI and tracking components.
 */
public interface UsageTrackingManagementService {

  /**
   * Returns the service bound to this interface in the plugin.xml
   */
  static UsageTrackingManagementService getInstance() {
    return ServiceManager.getService(UsageTrackingManagementService.class);
  }

  /**
   * Sets the user's preference for whether anonymous usage data should be collected or not.
   *
   * @param optIn {@code true} to turn on usage collection or {@code false} otherwise
   */
  void setTrackingPreference(boolean optIn);

  /**
   * Returns whether the user is opted in to anonymous usage monitoring or not.
   */
  boolean hasUserOptedIn();

  /**
   * This method is not to be confused with {@link #hasUserOptedIn()} because it only indicates
   * whether the user has set a preference for providing anonymous usage data.
   *
   * @return {@code true} if the user has either consented or declined to provide anonymous usage
   *        data, and {@code false} if the user has never indicated a preference.
   */
  boolean hasUserRecordedTrackingPreference();

  /**
   * Indicates whether usage tracking is configured for this plugin's release and platform. This is
   * independent of whether the user is opted in to usage tracking.
   *
   * <p>{@code isUsageTrackingAvailable()} and {@link #hasUserOptedIn()} both need to return {@code
   * true} for tracking to be enabled. Call {@link #isTrackingEnabled()} to determine whether to do
   * user tracking.
   */
  boolean isUsageTrackingAvailable();

  /**
   * Returns the plugin's analytics ID.
   */
  @Nullable
  String getAnalyticsProperty();

  /**
   * Returns whether tracking is turned on.  This means that the user has explicitly consented to
   * anonymous usage tracking and the plugin is configured with analytics turned on so that usage
   * tracking is possible.
   */
  boolean isTrackingEnabled();
}
