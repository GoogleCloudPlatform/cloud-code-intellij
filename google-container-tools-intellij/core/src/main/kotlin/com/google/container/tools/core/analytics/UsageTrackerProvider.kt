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

import com.google.cloud.tools.ide.analytics.UsageTracker
import com.google.cloud.tools.ide.analytics.UsageTrackerManager
import com.google.cloud.tools.ide.analytics.UsageTrackerSettings
import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.core.PluginInfo
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.components.ServiceManager

/**
 * Application service that initializes the [UsageTracker] for analytics. Sets up core components
 * of analytics collection such as the analytics ID, user agent, client ID etc.
 *
 * Example usage:
 * ```
 * UsageTrackerProvider.instance.usageTracker.trackEvent("event-name").ping()
 * ```
 */
class UsageTrackerProvider {

    @VisibleForTesting
    var usageTrackerSettings: UsageTrackerSettings

    init {
        val usageTrackerManagerService: UsageTrackerManagerService =
            UsageTrackerManagerService.instance

        usageTrackerSettings = UsageTrackerSettings.Builder()
            .manager { usageTrackerManagerService.isUsageTrackingEnabled() }
            .analyticsId(usageTrackerManagerService.getAnalyticsId())
            .pageHost(PAGE_HOST)
            .platformName(PluginInfo.instance.platformPrefix)
            .platformVersion(ApplicationInfo.getInstance().strictVersion)
            .pluginName(PluginInfo.PLUGIN_NAME_EXTERNAL)
            .pluginVersion(PluginInfo.instance.pluginVersion)
            .clientId(PermanentInstallationID.get())
            .userAgent(PluginInfo.PLUGIN_USER_AGENT)
            .build()
    }

    /**
     * Returns the appropriate implementation of [UsageTracker]. The implementation will change -
     * from either a noop version, or the real version - depending on the value returned by the
     * [UsageTrackerManager] callback which will depend on the current user selected tracking
     * preference.
     */
    val usageTracker: UsageTracker get() = UsageTracker.create(usageTrackerSettings)

    companion object {
        private const val PAGE_HOST = "virtual.intellij"

        val instance
            get() = ServiceManager.getService(UsageTrackerProvider::class.java)!!
    }
}
