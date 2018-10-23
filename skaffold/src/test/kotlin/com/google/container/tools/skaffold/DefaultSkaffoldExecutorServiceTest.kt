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

package com.google.container.tools.skaffold

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.test.ContainerToolsRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Unit tests for [DefaultSkaffoldExecutorService] and [SkaffoldExecutorService] */
class DefaultSkaffoldExecutorServiceTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var defaultSkaffoldExecutorService: DefaultSkaffoldExecutorService

    @MockK
    private lateinit var mockProcess: Process

    @Before
    fun setUp() {
        defaultSkaffoldExecutorService = spyk(DefaultSkaffoldExecutorService())
        every { defaultSkaffoldExecutorService.createProcess(any(), any()) } answers { mockProcess }
    }

    @Test
    fun `single run with no arguments launches skaffold run`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.SINGLE_RUN
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold run")
    }

    @Test
    fun `dev mode with no arguments launches skaffold dev`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev")
    }

    @Test
    fun `skaffold config filename argument generates valid skaffold filename flag`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml"
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev --filename test.yaml")
    }

    @Test
    fun `working directory is passed on to process builder`() {
        defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml",
                workingDirectory = File("/tmp")
            )
        )

        verify { defaultSkaffoldExecutorService.createProcess(File("/tmp"), any()) }
    }
}
