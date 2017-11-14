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

/**
 * Interface for defining the actual tracking behavior, implementations must be declared in
 * plugin.xml for the {@link UsageTrackerExtensionPointBean} extension point.
 */
public interface UsageTracker {

  /**
   * Returns a fluent API for pinging tracking events.
   *
   * <p>Example: {@code sendEvent("appengine.deployment").withLabel("flex").setValue(1).ping();}
   *
   * @param action is typically a specific operation the user has performed in the plugin, and is
   *               often prefixed with a domain such as 'appengine.' or 'clouddebugger.'
   * @return a fluent interface for setting the remaining parameters of a tracking ping
   */
  FluentTrackingEventWithMetadata trackEvent(String action);

  /**
   * Part of the tracking event fluent API. Denotes steps in the API where the event has enough data
   * to ping a ping.
   */
  interface PingsAnalytics {

    /**
     * Send the analytics ping.
     */
    void ping();
  }

  /**
   * Interface that accepts a key/value metadata pair for pinging tracking events.
   */
  interface FluentTrackingEventWithMetadata extends PingsAnalytics {

    /**
     * Sets an arbitrary key/value pair representing metadata for this event.
     *
     * @param key the key used for this event metadata
     * @param value the value used for this event metadata
     * @return this fluent interface for further setting of event metadata
     */
    FluentTrackingEventWithMetadata addMetadata(String key, String value);
  }
}
