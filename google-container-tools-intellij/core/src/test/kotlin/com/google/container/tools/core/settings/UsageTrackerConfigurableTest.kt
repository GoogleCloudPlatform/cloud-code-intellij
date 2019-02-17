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
import com.google.container.tools.core.analytics.UsageTrackerManagerService
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.google.container.tools.test.UiTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [UsageTrackerConfigurable]
 */
class UsageTrackerConfigurableTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @TestService
    @MockK
    private lateinit var usageTrackerManagerService: UsageTrackerManagerService

    private lateinit var usageTrackerConfigurable: UsageTrackerConfigurable

    @Before
    fun setUp() {
        usageTrackerConfigurable = UsageTrackerConfigurable()
    }

    @Test
    @UiTest
    fun `isModified is true when stored preference does not match checkbox selection`() {
        // stored value is false
        every { usageTrackerManagerService.isTrackingOptedIn() } answers { false }
        // preference is selected
        usageTrackerConfigurable.usageTrackerCheckbox.isSelected = true

        assertThat(usageTrackerConfigurable.isModified).isTrue()
    }

    @Test
    @UiTest
    fun `isModified is false when stored preference matches checkbox selection`() {
        // stored value is true
        every { usageTrackerManagerService.isTrackingOptedIn() } answers { true }
        // preference is selected
        usageTrackerConfigurable.usageTrackerCheckbox.isSelected = true

        assertThat(usageTrackerConfigurable.isModified).isFalse()
    }

    @Test
    @UiTest
    fun `when preference checkbox is selected apply stores the tracking selection`() {
        usageTrackerConfigurable.usageTrackerCheckbox.isSelected = true
        usageTrackerConfigurable.apply()

        verify { usageTrackerManagerService.setTrackingOptedIn(true) }
    }

    @Test
    @UiTest
    fun `reset restores the persisted tracking preference to the checkbox selection`() {
        // First set the checkbox to unselected
        usageTrackerConfigurable.usageTrackerCheckbox.isSelected = false

        // Set the persisted value to true
        every { usageTrackerManagerService.isTrackingOptedIn() } answers { true }

        usageTrackerConfigurable.reset()

        assertThat(usageTrackerConfigurable.usageTrackerCheckbox.isSelected).isTrue()
    }
}
