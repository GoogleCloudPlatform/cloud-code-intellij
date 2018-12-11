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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager

/**
 * Application service that manages the status of usage tracking in the plugin.
 */
class UsageTrackerManagerService {

    companion object {
        private const val ANALYTICS_ID_KEY = "analytics.id"
        private const val ANALYTICS_ID_PLACEHOLDER_VAL = "\${analyticsId}"

        val instance
            get() = ServiceManager.getService(UsageTrackerManagerService::class.java)!!
    }

    /**
     * Returns true if usage tracking is enabled, and false otherwise. Usage tracking is enabled if
     * the Analytics ID is configured and we are not running from unit test mode (we don't want to
     * send pings from unit tests).
     *
     * TODO (next PR): add in bit that checks if the user has not opted out of tracking
     */
    fun isUsageTrackingEnabled() =
        getAnalyticsId() != null && !ApplicationManager.getApplication().isUnitTestMode

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
}
