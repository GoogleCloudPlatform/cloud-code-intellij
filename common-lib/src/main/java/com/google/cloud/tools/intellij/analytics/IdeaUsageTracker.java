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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Utility class to send client-side, opt-in usage events to Google Analytics (GA).
 * <p/>
 * Actual Tracking ID of a target GA Property (ANALYTICS_TRACKING_ID) will be read from a config
 * property "usage.analytics.tracking.id". (Note that we don't hardcode the Tracking ID in our
 * codebase. See "google-account-plugin/build.gradle" for how we set the property when we release
 * our plugin.)
 */
public class IdeaUsageTracker {

  private static final Logger logger = Logger.getInstance(IdeaUsageTracker.class);

  // UUID for this IntelliJ installation. Will be used to uniquely identify a user.
  private static final String clientUuid =
      UpdateChecker.getInstallationUID(PropertiesComponent.getInstance());

  private String ideaType;
  private String analyticsTrackingId;
  private Predicate<Object> optedIn;

  /**
   * Initialize the tracker.
   *
   * @param ideaTypePostfix String indicating IntelliJ IDEA type (e.g., Community Edition or
   *      Ultimate Edition) appended to the event type string reported to GA.
   * @param analyticsTrackingId In our IntelliJ codebase, the Tracking ID is supposed to come from
   *     "google-account-plugin/resources/config.properties". The property name is
   *     "usage.analytics.tracking.id".
   * @param optedIn A predicate (effectively an 0-argument lambda function) returning if a user has
   *     opted in for client-side usage reporting.
   */
  public void init(@Nullable String ideaTypePostfix,
      @Nullable String analyticsTrackingId, @Nullable Predicate<Object> optedIn) {
    // Do the basic checking to see if "google-account-plugin/build.gradle" has expanded the
    // Tracking ID property. Analytics Tracking ID has the form of "UA-xxxxxxxx-y"
    // (https://support.google.com/analytics/answer/1032385?hl=en).
    if (analyticsTrackingId != null && analyticsTrackingId.startsWith("UA-")) {
      Preconditions.checkNotNull(optedIn);

      this.analyticsTrackingId = analyticsTrackingId;
      this.optedIn = optedIn;
      this.ideaType = "gcloud.intellij." + (ideaTypePostfix == null ? "unknown" : ideaTypePostfix);
    }
  }

  /**
   * Send an opt-in usage event to Google Analytics. Do not send any PII.
   */
  public void trackEvent(@NotNull String eventName, @Nullable Map<String, String> metadata) {
    if (analyticsTrackingId != null && optedIn != null && optedIn.apply(null)) {
      try {
        new MetricsSender(analyticsTrackingId).send(buildEvent(eventName, metadata));
      } catch (MetricsException me) {
        logger.debug(me);
      }
    }
  }

  public void trackEvent(@NotNull String eventName) {
    trackEvent(eventName, null);
  }

  public void trackEvent(
      @NotNull String eventName, @NotNull String metadataKey, @NotNull String metadataValue) {
    trackEvent(eventName, ImmutableMap.<String, String>of(metadataKey, metadataValue));
  }

  @VisibleForTesting
  Event buildEvent(@NotNull String eventName, @Nullable Map<String, String> metadata) {
    Event.Builder builder = Event.builder();
    builder.setClientId(clientUuid);
    builder.setType(ideaType);
    builder.setName(eventName);
    builder.setIsUserSignedIn(false);
    builder.setIsUserInternal(false);
    if (metadata != null) {
      for (Map.Entry<String, String> entry : metadata.entrySet()) {
        builder.addMetadata(entry.getKey(), entry.getValue());
      }
    }

    return builder.build();
  }
}
