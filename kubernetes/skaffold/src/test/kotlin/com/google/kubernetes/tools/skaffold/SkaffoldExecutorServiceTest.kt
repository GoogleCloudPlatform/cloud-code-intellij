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

package com.google.kubernetes.tools.skaffold

import com.google.common.truth.Truth.assertThat
import com.google.kubernetes.tools.test.ContainerToolsRule
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SkaffoldExecutorServiceTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var defaultSkaffoldExecutorService: DefaultSkaffoldExecutorService

    @MockK
    private lateinit var mockProcess: Process

    @MockK
    private lateinit var mockSkaffoldProcess: SkaffoldProcess

    @Before
    fun setUp() {
        defaultSkaffoldExecutorService = spyk(DefaultSkaffoldExecutorService())
        every { defaultSkaffoldExecutorService.createProcess(any(), any()) } answers { mockProcess }
    }

    @Test
    fun `isSkaffoldAvailable returns true when skaffold execution returns 0 exit code`() {
        mockSkaffoldExecution()
        every { mockProcess.exitValue() } answers { 0 }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isTrue()
    }

    @Test
    fun `isSkaffoldAvailable returns false when skaffold execution returns 1 exit code`() {
        mockSkaffoldExecution()
        every { mockProcess.exitValue() } answers { 1 }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isFalse()
    }

    @Test
    fun `isSkaffoldAvailable returns true when skaffold execution times out`() {
        mockSkaffoldExecution()
        every { mockProcess.waitFor(any(), any()) } answers { false }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isTrue()
    }

    @Test
    fun `isSkaffoldAvailable returns false when skaffold execution throws exception`() {
        mockSkaffoldExecution()
        every { defaultSkaffoldExecutorService.executeSkaffold(any()) } throws Exception()
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isFalse()
    }

    private fun mockSkaffoldExecution() {
        every {
            defaultSkaffoldExecutorService.executeSkaffold(any())
        } answers { mockSkaffoldProcess }

        every { mockSkaffoldProcess.process } answers { mockProcess }
        every { mockProcess.waitFor(any(), any()) } answers { true }
    }
}