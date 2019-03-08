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

package com.google.kubernetes.tools.settings

import com.google.common.truth.Truth
import com.google.kubernetes.tools.core.settings.KubernetesSettingsService
import com.google.kubernetes.tools.test.ContainerToolsRule
import com.google.kubernetes.tools.test.TestService
import com.google.kubernetes.tools.test.UiTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [KubernetesSettingsConfigurable].
 */
class KubernetesSettingsConfigurableTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @TestService
    @MockK
    private lateinit var kubernetesSettingsService: KubernetesSettingsService

    private lateinit var kubernetesSettingsConfigurable: KubernetesSettingsConfigurable

    @Before
    fun setUp() {
        kubernetesSettingsConfigurable = KubernetesSettingsConfigurable()
    }

    @Test
    @UiTest
    fun `reset with stored skaffold path populates skaffold browser`() {
        val skaffoldPath = "/path/to/skaffold"
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { skaffoldPath }

        kubernetesSettingsConfigurable.reset()

        Truth.assertThat(kubernetesSettingsConfigurable.skaffoldBrowser.text)
                .isEqualTo(skaffoldPath)
    }

    @Test
    @UiTest
    fun `reset with empty skaffold path populates skaffold browser with empty strign`() {
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { "" }

        kubernetesSettingsConfigurable.reset()

        Truth.assertThat(kubernetesSettingsConfigurable.skaffoldBrowser.text)
                .isEqualTo("")
    }

    @Test
    @UiTest
    fun `isModified returns true when browser text differs from persisted value`() {
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { "/path/1/skaffold" }
        kubernetesSettingsConfigurable.skaffoldBrowser.text = "/path/2/skaffold"

        Truth.assertThat(kubernetesSettingsConfigurable.isModified).isTrue()
    }

    @Test
    @UiTest
    fun `isModified returns false when browser text matches persisted value`() {
        val matchingSkaffoldPath = "/path/to/skaffold"
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { matchingSkaffoldPath }
        kubernetesSettingsConfigurable.skaffoldBrowser.text = matchingSkaffoldPath

        Truth.assertThat(kubernetesSettingsConfigurable.isModified).isFalse()
    }

    @Test
    @UiTest
    fun `isModified returns false when all values are uninitialized`() {
        Truth.assertThat(kubernetesSettingsConfigurable.isModified).isFalse()
    }

    @Test
    @UiTest
    fun `isModified returns false when persisted value is empty and browser is uninitialized`() {
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { "" }

        Truth.assertThat(kubernetesSettingsConfigurable.isModified).isFalse()
    }

    @Test
    @UiTest
    fun `isModified returns false when persisted value is unitialized and browser text is empty`() {
        kubernetesSettingsConfigurable.skaffoldBrowser.text = ""

        Truth.assertThat(kubernetesSettingsConfigurable.isModified).isFalse()
    }
}
