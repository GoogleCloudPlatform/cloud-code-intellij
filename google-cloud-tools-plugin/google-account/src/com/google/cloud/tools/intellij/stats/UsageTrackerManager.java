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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.login.PluginFlags;
import com.google.cloud.tools.intellij.login.PropertiesFilePluginFlags;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Stores the user's choice to opt in/out of sending usage metrics via the Google Usage Tracker.
 */
// TODO: Refactor to an IntelliJ service
public final class UsageTrackerManager {
  @VisibleForTesting
  static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";
  @VisibleForTesting
  static final String USAGE_TRACKER_PROPERTY_PLACEHOLDER = "${usageTrackerProperty}";
  private static UsageTrackerManager instance;
  private PropertiesComponent datastore;
  private PluginFlags flags;
  private static final Object factoryLock = new Object();

  private UsageTrackerManager() {
    datastore = PropertiesComponent.getInstance();
    flags = new PropertiesFilePluginFlags();
  }

  @VisibleForTesting
  UsageTrackerManager(PropertiesComponent propertiesComponent, PluginFlags flags) {
    this.datastore = propertiesComponent;
    this.flags = flags;
  }

  /**
   * Return an instance of this manager.
   */
  public static UsageTrackerManager getInstance() {
    synchronized (factoryLock) {
      if (instance == null) {
        instance = new UsageTrackerManager();
      }
      return instance;
    }
  }

  public void setTrackingPreference(boolean optIn) {
    datastore.setValue(USAGE_TRACKER_KEY, String.valueOf(optIn));
  }

  public boolean hasUserOptedIn() {
    return datastore.getBoolean(USAGE_TRACKER_KEY, false);
  }

  /**
   * Returns {@code true} if the user has opted in or out of usage tracking, and {@code false} if
   * the user has yet to indicate a tracking preference.
   */
  public boolean hasUserRecordedTrackingPreference() {
    return datastore.getValue(USAGE_TRACKER_KEY) != null;
  }

  /**
   * Indicates whether usage tracking is configured for this plugin's release and platform.  This is
   * independent of whether the user is opted in to usage tracking.
   *
   * {@code isUsageTrackingAvailable()} and {@link #hasUserOptedIn()} both need to return
   * {@code true} for tracking to be enabled. Call {@link #isTrackingEnabled()} to determine whether
   * to do user tracking.
   */
  public boolean isUsageTrackingAvailable() {
    return PlatformUtils.isIntelliJ() && (getAnalyticsProperty() != null);
  }

  @Nullable
  protected String getAnalyticsProperty() {
    String analyticsId = flags.getAnalyticsId();
    if (!USAGE_TRACKER_PROPERTY_PLACEHOLDER.equals(analyticsId)) {
      return analyticsId;
    }
    return null;
  }

  public boolean isTrackingEnabled() {
    return isUsageTrackingAvailable() && hasUserOptedIn();
  }
}
