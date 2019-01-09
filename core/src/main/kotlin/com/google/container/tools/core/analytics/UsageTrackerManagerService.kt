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

package com.google.container.tools.core.analytics

import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.core.properties.PluginPropertiesFileReader
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

/**
 * Application service that manages the status of usage tracking in the plugin.
 *
 * @param trackingPreferenceProperty the [PropertiesComponent] used for persistence. Useful for
 *  swapping out mocks for unit testing.
 */
class UsageTrackerManagerService(
    private val trackingPreferenceProperty: PropertiesComponent = PropertiesComponent.getInstance()
) {

    companion object {
        private const val ANALYTICS_ID_KEY = "analytics.id"
        private const val ANALYTICS_ID_PLACEHOLDER_VAL = "\${analyticsId}"

        // This key needs to match the key used by the Google Cloud Tools plugin. Do not change
        // unless certain.
        private const val USAGE_TRACKING_PREFERENCE_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN"

        val instance
            get() = ServiceManager.getService(UsageTrackerManagerService::class.java)!!
    }

    /**
     * Tracking is considered "available" if it can be collected. This occurs when there is a
     * retrievable Analytics ID, and we are not in unit test mode.
     */
    fun isUsageTrackingAvailable(): Boolean =
        getAnalyticsId() != null && !ApplicationManager.getApplication().isUnitTestMode

    /**
     * Tracking is considered "enabled" if [isUsageTrackingAvailable] is true AND the user has
     * opted into tracking.
     */
    fun isUsageTrackingEnabled(): Boolean = isUsageTrackingAvailable() && isTrackingOptedIn()

    /**
     * Returns the analytics ID as configured in the properties file. If the analytics ID
     * placeholder has not been substituted, then this will return null.
     */
    @VisibleForTesting
    fun getAnalyticsId(): String? {
        val analyticsId: String? =
            PluginPropertiesFileReader.instance.getPropertyValue(ANALYTICS_ID_KEY)

        return if (ANALYTICS_ID_PLACEHOLDER_VAL != analyticsId) analyticsId else null
    }

    /**
     * Stores the user tracking preference backed by a persistent [PropertiesComponent].
     */
    fun setTrackingOptedIn(optIn: Boolean) {
        trackingPreferenceProperty.setValue(USAGE_TRACKING_PREFERENCE_KEY, optIn)
    }

    /**
     * Returns true if the user has opted in to tracking, and false if the user has opted out.
     */
    fun isTrackingOptedIn(): Boolean =
        trackingPreferenceProperty.getBoolean(USAGE_TRACKING_PREFERENCE_KEY, false)
}
