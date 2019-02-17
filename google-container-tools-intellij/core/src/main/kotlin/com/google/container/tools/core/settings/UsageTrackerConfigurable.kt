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

import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.core.analytics.UsageTrackerManagerService
import com.google.container.tools.core.util.CoreBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.options.Configurable
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.layout.panel
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JComponent

/**
 * Creates the "Usage Tracking" menu item in Settings under the top-level "Google" item. This allows
 * users to opt in or out of usage tracking via the settings menu.
 *
 * The setting is backed by a persistent [PropertiesComponent] which stores the user preference.
 * This preference is shared between this plugin and the Google Cloud Tools plugin (and potentially
 * future Google plugins).
 *
 * Multiple Google plugins may attempt to provide this menu item. The platform will ensure that only
 * one is created as long as the IDs match - as configured in the plugin xml settings. Regardless of
 * which plugin provides this configurable, they will all persist the user preference to the same
 * shared key.
 */
class UsageTrackerConfigurable : Configurable {

    @VisibleForTesting
    val usageTrackerCheckbox =
        JCheckBox(CoreBundle.message("usage.tracking.preference.checkbox.text"))

    override fun getDisplayName(): String =
        CoreBundle.message("usage.tracking.settings.menu.item.text")

    /**
     * The panel is considered modified if the stored tracking preference does not match the current
     * value in the checkbox.
     */
    override fun isModified(): Boolean =
        UsageTrackerManagerService.instance.isTrackingOptedIn() !=
            usageTrackerCheckbox.isSelected

    /**
     * Stores the user preference via the [UsageTrackerManagerService] which persists the setting.
     */
    override fun apply() {
        UsageTrackerManagerService.instance.setTrackingOptedIn(usageTrackerCheckbox.isSelected)
    }

    /**
     * Resets/initializes the setting based on the persisted value.
     */
    override fun reset() {
        usageTrackerCheckbox.isSelected =
            UsageTrackerManagerService.instance.isTrackingOptedIn()
    }

    /**
     * Creates the UI panel with the checkbox for the user to opt in or out of usage tracking.
     */
    override fun createComponent(): JComponent? {
        val usageTrackingPanel = panel {
            row {
                usageTrackerCheckbox()
                noteRow(CoreBundle.message("usage.tracker.panel.privacy.notice-text"))
            }
        }

        usageTrackingPanel.border = IdeaTitledBorder(
            CoreBundle.message("usage.tracking.panel.title"),
            0,
            Insets(0, 0, 0, 0)
        )

        return panel {
            row {
                usageTrackingPanel()
            }
        }
    }
}
