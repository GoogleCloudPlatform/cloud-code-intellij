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

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.core.properties.PluginPropertiesFileReader
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [UsageTrackerConfigurableProvider]
 */
class UsageTrackerConfigurableProviderTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @TestService
    @MockK
    private lateinit var propertyReader: PluginPropertiesFileReader

    private lateinit var usageTrackerConfigurableProvider: UsageTrackerConfigurableProvider

    @Before
    fun setUp() {
        usageTrackerConfigurableProvider = UsageTrackerConfigurableProvider()
    }

    @Test
    fun `usage tracker panel is not available in unit test mode`() {
        // Set a valid looking analytics ID so that we are only testing the unit test mode part
        every { propertyReader.getPropertyValue("analytics.id") } answers { "UA-12345" }

        assertThat(usageTrackerConfigurableProvider.canCreateConfigurable()).isFalse()
    }
}
