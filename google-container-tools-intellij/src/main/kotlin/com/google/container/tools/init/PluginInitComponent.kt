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

package com.google.container.tools.init

import com.google.container.tools.core.PluginInfo
import com.google.container.tools.core.analytics.UsageTrackerManagerService
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.extensions.PluginId

/**
 * Performs initialization tasks when the plugin is loaded.
 */
class PluginInitComponent : BaseComponent {

    override fun initComponent() {
        val isGctInstalled =
            PluginManager.isPluginInstalled(PluginId.getId(PluginInfo.GOOGLE_CLOUD_TOOLS_PLUGIN_ID))

        /* Initialize feature usage tracking. Only do this if:
           1. The Google Cloud Tools plugin is not installed
           2. Usage tracking is available
           3. A tracking preference has not already been recorded
         */
        if (!isGctInstalled &&
            UsageTrackerManagerService.instance.isUsageTrackingAvailable() &&
            !UsageTrackerManagerService.instance.hasUserRecordedTrackingPreference()
        ) {
            UsageTrackerManagerService.instance.setTrackingOptedIn(true)
        }
    }
}
