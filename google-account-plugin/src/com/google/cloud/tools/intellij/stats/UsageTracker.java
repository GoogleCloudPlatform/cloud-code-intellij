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
  PartialTrackingEventLabel trackEvent(String action);

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
   * Interface that accepts the 'label' optional field for pinging tracking events.
   */
  interface PartialTrackingEventLabel extends PingsAnalytics {

    /**
     * Sets the optional 'label' field.
     *
     * @param label adds metadata about the 'action' being performed. For example an action of
     *              'appengine.deploy', could qualify the deployment as a flex deployment by passing
     *              'flex' as the {@code label} value.
     * @return a fluent interface for setting a scalar value attributed to the parameters of the
     *         tracking ping
     */
    PartialTrackingEventValue withLabel(String label);

    /**
     * Interface that accepts a scalar Integer value as a metric for the analytics ping.
     */
    interface PartialTrackingEventValue extends PingsAnalytics {

      /**
       * Sets the optional scalar value to be associated with this tracking event.
       *
       * @param value an optional scalar value that will be recorded as a metric against this
       *              tracking event
       * @return a fluent interface for sending the tracking event ping
       */
      PingsAnalytics setValue(Integer value);
    }
  }
}
