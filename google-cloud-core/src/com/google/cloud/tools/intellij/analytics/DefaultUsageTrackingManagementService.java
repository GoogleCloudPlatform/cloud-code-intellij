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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.login.PluginFlagsService;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/** Stores the user's choice to opt in/out of sending usage metrics via the Google Usage Tracker. */
public final class DefaultUsageTrackingManagementService implements UsageTrackingManagementService {
  @VisibleForTesting
  static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";

  @VisibleForTesting
  static final String USAGE_TRACKER_PROPERTY_PLACEHOLDER = "${usageTrackerProperty}";

  private PropertiesComponent datastore;
  private PluginFlagsService flags;

  public DefaultUsageTrackingManagementService() {
    datastore = PropertiesComponent.getInstance();
    flags = PluginFlagsService.getInstance();
  }

  @VisibleForTesting
  DefaultUsageTrackingManagementService(
      PropertiesComponent propertiesComponent, PluginFlagsService flags) {
    this.datastore = propertiesComponent;
    this.flags = flags;
  }

  @Override
  public void setTrackingPreference(boolean optIn) {
    datastore.setValue(USAGE_TRACKER_KEY, String.valueOf(optIn));
  }

  @Override
  public boolean hasUserOptedIn() {
    return datastore.getBoolean(USAGE_TRACKER_KEY, false);
  }

  @Override
  public boolean hasUserRecordedTrackingPreference() {
    return datastore.getValue(USAGE_TRACKER_KEY) != null;
  }

  /**
   * Returns {@code true} if usage tracking is available. Usage tracking is excluded from Android
   * Studio.
   */
  @Override
  public boolean isUsageTrackingAvailable() {
    return !PlatformUtils.getPlatformPrefix().equals("AndroidStudio")
        && getAnalyticsProperty() != null;
  }

  @Override
  @Nullable
  public String getAnalyticsProperty() {
    String analyticsId = flags.getAnalyticsId();
    if (!USAGE_TRACKER_PROPERTY_PLACEHOLDER.equals(analyticsId)) {
      return analyticsId;
    }
    return null;
  }

  @Override
  public boolean isTrackingEnabled() {
    return isUsageTrackingAvailable() && hasUserOptedIn();
  }
}
