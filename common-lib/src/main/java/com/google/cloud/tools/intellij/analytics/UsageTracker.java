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

package com.google.cloud.tools.intellij.analytics;

import com.google.cloud.metrics.Event;
import com.google.cloud.metrics.MetricsException;
import com.google.cloud.metrics.MetricsSender;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.common.collect.ImmutableMap;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class UsageTracker {

  private static final Logger LOGGER = Logger.getInstance(UsageTracker.class);

  private static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";
  private static final String ANALYTICS_TRACKING_ID;

  // UUID for this IntelliJ installation. Will be used to uniquely identify a user.
  private static final String clientUuid =
      UpdateChecker.getInstallationUID(PropertiesComponent.getInstance());

  static {
    String id = new PropertiesFileFlagReader().getFlagString("usage.analytics.tracking.id");

    // Do the basic checking to see if build.gradle expanded the resource property.
    // Analytics Tracking ID has the form of "UA-xxxxxxxx-y" (https://support.google.com/analytics/answer/1032385?hl=en).
    ANALYTICS_TRACKING_ID = (id != null && id.startsWith("UA-")) ? id : null;
  }

  /**
   * Send an opt-in usage event to Google Analytics. Do not send any PII.
   *
   */
  public static void trackEvent(@NotNull String eventType, @NotNull String eventName,
                                @Nullable Map<String, String> metadata) {
    PropertiesComponent datastore = PropertiesComponent.getInstance();
    boolean optedIn = datastore.getBoolean(USAGE_TRACKER_KEY, false);

    if (optedIn && ANALYTICS_TRACKING_ID != null) {
      Event.Builder builder = Event.builder();
      builder.setClientId(clientUuid);
      builder.setType(eventType);
      builder.setName(eventName);
      builder.setIsUserSignedIn(false);
      builder.setIsUserInternal(false);
      if (metadata != null) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
          builder.addMetadata(entry.getKey(), entry.getValue());
        }
      }

      try {
        new MetricsSender(ANALYTICS_TRACKING_ID).send(builder.build());
      } catch (MetricsException me) {
        LOGGER.debug(me);
      }
    }
  }

  public static void trackEvent(@NotNull String eventType, @NotNull String eventName) {
    trackEvent(eventType, eventName, null);
  }

  public static void trackEvent(@NotNull String eventType, @NotNull String eventName,
                                @NotNull String metadataKey, @NotNull String metadataValue) {
    trackEvent(eventType, eventName, ImmutableMap.<String, String>of(metadataKey, metadataValue));
  }
}
