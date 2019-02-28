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

package com.google.kubernetes.tools.skaffold.run

import com.google.common.truth.Truth
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.expectThrows
import com.google.kubernetes.tools.skaffold.SkaffoldExecutorSettings
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.ThrowableRunnable
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SkaffoldRunConfigurationsTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @MockK
    private lateinit var executionEnvironment: ExecutionEnvironment
    @MockK
    private lateinit var mockRunExecutor: DefaultRunExecutor
    @MockK
    private lateinit var mockDebugExecutor: DefaultDebugExecutor
    @MockK
    private lateinit var mockExecutor: Executor

    private lateinit var skaffoldDevConfiguration: SkaffoldDevConfiguration
    private lateinit var skaffoldSingleRunConfiguration: SkaffoldSingleRunConfiguration

    @Before
    fun setUp() {
        skaffoldDevConfiguration = SkaffoldDevConfiguration(
                containerToolsRule.ideaProjectTestFixture.project,
                SkaffoldDevConfigurationFactory(SkaffoldRunConfigurationType()),
                "dev-config")
        skaffoldSingleRunConfiguration = SkaffoldSingleRunConfiguration(
                containerToolsRule.ideaProjectTestFixture.project,
                SkaffoldSingleRunConfigurationFactory(SkaffoldRunConfigurationType()),
                "run-config")
    }

    @Test
    fun `Skaffold dev with a debug executor uses debug execution mode`() {
        every { executionEnvironment.executor } answers { mockDebugExecutor }
        Truth.assertThat(skaffoldDevConfiguration.getExecutionMode(executionEnvironment))
                .isEqualTo(SkaffoldExecutorSettings.ExecutionMode.DEBUG)
    }

    @Test
    fun `Skaffold dev with a run executor uses dev execution mode`() {
        every { executionEnvironment.executor } answers { mockRunExecutor }
        Truth.assertThat(skaffoldDevConfiguration.getExecutionMode(executionEnvironment))
                .isEqualTo(SkaffoldExecutorSettings.ExecutionMode.DEV)
    }

    @Test
    fun `Skaffold dev with an unrecognized executor throws exception`() {
        every { executionEnvironment.executor } answers { mockExecutor }
        expectThrows(
                RuntimeException::class,
                ThrowableRunnable { skaffoldDevConfiguration
                        .getExecutionMode(executionEnvironment) })
    }
}