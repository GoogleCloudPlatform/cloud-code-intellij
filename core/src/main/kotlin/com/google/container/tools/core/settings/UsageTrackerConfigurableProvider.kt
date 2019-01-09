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

package com.google.container.tools.core.settings

import com.google.container.tools.core.analytics.UsageTrackerManagerService
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

private const val USAGE_TRACKER_PROVIDER_NAME = "UsageTrackerConfigurableProvider"

/**
 * Class that provides the configurable which creates the "Usage Tracking" menu item under the
 * top-level "Google" menu item.
 */
class UsageTrackerConfigurableProvider : ConfigurableProvider() {

    override fun createConfigurable(): Configurable? = UsageTrackerConfigurable()

    /**
     * Only create the menu item if usage tracking is available. For example, if running in dev
     * mode with no analytics ID environment variable configured, hide the usage track menu item.
     */
    override fun canCreateConfigurable(): Boolean {
        /**
         * This check is a workaround for the fact that the older versions of the GCT plugin do not
         * have an ID configured for this panel. Since we only want a single instance of this panel
         * to appear for all installed Google plugins, this checks to ensure that it was not already
         * registered by the GCT plugin. Once all users migrate to newer versions of the GCT plugin,
         * we can remove this. For this to work, both provider classes have to have the same name.
         */
        val canCreateConfigurable: Boolean =
            Configurable.APPLICATION_CONFIGURABLE.extensionList.filter {
                it?.providerClass != null &&
                    it.providerClass.endsWith(USAGE_TRACKER_PROVIDER_NAME)
            }.size == 1

        return canCreateConfigurable &&
            UsageTrackerManagerService.instance.isUsageTrackingAvailable()
    }
}
