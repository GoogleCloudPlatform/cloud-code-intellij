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

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.core.properties.PluginPropertiesFileReader
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.intellij.ide.util.PropertiesComponent
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [UsageTrackerManagerService].
 */
class UsageTrackerManagerServiceTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @TestService
    @MockK
    private lateinit var propertyReader: PluginPropertiesFileReader

    @MockK
    private lateinit var trackingPreferenceProperty: PropertiesComponent

    private lateinit var usageTrackerManagerService: UsageTrackerManagerService

    @Before
    fun setUp() {
        usageTrackerManagerService = UsageTrackerManagerService(trackingPreferenceProperty)
    }

    @Test
    fun `usage tracking is not available and not enabled in unit test mode`() {
        // Set the analytics ID to a proper value
        val analyticsId = "UA-12345"
        mockAnalyticsId(analyticsId)

        assertThat(usageTrackerManagerService.isUsageTrackingAvailable()).isFalse()
        assertThat(usageTrackerManagerService.isUsageTrackingEnabled()).isFalse()
    }

    @Test
    fun `when user data tracking preference is stored true then tracking is opted in`() {
        every {
            trackingPreferenceProperty.getBoolean(
                "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN",
                false
            )
        } answers { true }

        assertThat(usageTrackerManagerService.isTrackingOptedIn()).isTrue()
    }

    @Test
    fun `when user data tracking preference is stored false then tracking is not opted in`() {
        every {
            trackingPreferenceProperty.getBoolean(
                "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN",
                false
            )
        } answers { false }

        assertThat(usageTrackerManagerService.isTrackingOptedIn()).isFalse()
    }

    @Test
    fun `get analytics ID when ID has been substituted returns analytics ID`() {
        val analyticsId = "UA-12345"
        mockAnalyticsId(analyticsId)

        assertThat(usageTrackerManagerService.getAnalyticsId()).isEqualTo(analyticsId)
    }

    @Test
    fun `get analytics ID when property placeholder has not been substituted returns null`() {
        val analyticsIdPlaceholder = "\${analyticsId}"
        every { propertyReader.getPropertyValue("analytics.id") } answers { analyticsIdPlaceholder }

        assertThat(usageTrackerManagerService.getAnalyticsId()).isNull()
    }

    private fun mockAnalyticsId(analyticsId: String) {
        every { propertyReader.getPropertyValue("analytics.id") } answers { analyticsId }
    }
}
