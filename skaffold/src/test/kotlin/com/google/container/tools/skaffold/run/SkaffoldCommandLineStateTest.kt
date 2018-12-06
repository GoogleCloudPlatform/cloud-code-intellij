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

package com.google.container.tools.skaffold.run

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.core.PluginInfo
import com.google.container.tools.skaffold.SkaffoldExecutorService
import com.google.container.tools.skaffold.SkaffoldExecutorSettings
import com.google.container.tools.skaffold.SkaffoldProcess
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.google.container.tools.test.expectThrows
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SearchScopeProvider
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.guessProjectDir
import com.intellij.util.ThrowableRunnable
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.jdom.Element
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Unit tests for [SkaffoldCommandLineState] */
class SkaffoldCommandLineStateTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var skaffoldCommandLineState: SkaffoldCommandLineState

    @MockK
    private lateinit var mockExecutionEnvironment: ExecutionEnvironment
    @MockK
    private lateinit var mockRunnerSettings: RunnerAndConfigurationSettings
    @MockK
    private lateinit var mockDevConfiguration: SkaffoldDevConfiguration
    @MockK
    @TestService
    private lateinit var mockSkaffoldExecutorService: SkaffoldExecutorService
    @MockK
    @TestService
    private lateinit var mockPluginInfoService: PluginInfo

    private val skaffoldSettingsCapturingSlot: CapturingSlot<SkaffoldExecutorSettings> = slot()

    @Before
    fun setUp() {
        every {
            mockExecutionEnvironment.runnerAndConfigurationSettings
        } answers { mockRunnerSettings }

        every { mockRunnerSettings.configuration } answers { mockDevConfiguration }
        // pass project into the CLI state
        every { mockExecutionEnvironment.project } answers {
            containerToolsRule.ideaProjectTestFixture.project
        }

        // CommandLineState calls this static method, needs to be mocked
        mockkStatic(SearchScopeProvider::class)
        every { SearchScopeProvider.createSearchScope(any(), any()) } answers { mockk() }

        // Skaffold executor answer mocks
        val mockSkaffoldProcess: SkaffoldProcess = mockk(relaxed = true)
        every {
            mockSkaffoldExecutorService.executeSkaffold(capture(skaffoldSettingsCapturingSlot))
        } answers { mockSkaffoldProcess }
    }

    @Test
    fun `unsupported run configuration type throws execution exception`() {
        val invalidSkaffoldRunConfiguration = mockk<RunConfigurationBase<Element>>()
        every { mockRunnerSettings.configuration } answers { invalidSkaffoldRunConfiguration }

        skaffoldCommandLineState = SkaffoldCommandLineState(
            mockExecutionEnvironment,
            SkaffoldExecutorSettings.ExecutionMode.DEV
        )

        val exception = expectThrows(
            ExecutionException::class,
            ThrowableRunnable { skaffoldCommandLineState.startProcess() })
        assertThat(exception.message).isEqualTo(
            "Your Skaffold run configuration is corrupted. Please re-create it to fix."
        )
    }

    @Test
    fun `null Skaffold config file results in execution exception`() {
        every { mockDevConfiguration.skaffoldConfigurationFilePath } answers { null }

        skaffoldCommandLineState = SkaffoldCommandLineState(
            mockExecutionEnvironment,
            SkaffoldExecutorSettings.ExecutionMode.DEV
        )

        val exception = expectThrows(
            ExecutionException::class,
            ThrowableRunnable { skaffoldCommandLineState.startProcess() })
        assertThat(exception.message).isEqualTo("Skaffold configuration file is not selected.")
    }

    @Test
    fun `given valid settings skaffold process is executed from the project directory`() {
        val projectBaseDir: String =
            containerToolsRule.ideaProjectTestFixture.project.guessProjectDir()!!.path
        every { mockDevConfiguration.skaffoldConfigurationFilePath } answers {
            projectBaseDir + "/skaffold.yaml"
        }

        skaffoldCommandLineState = SkaffoldCommandLineState(
            mockExecutionEnvironment,
            SkaffoldExecutorSettings.ExecutionMode.DEV
        )
        skaffoldCommandLineState.startProcess()

        assertThat(skaffoldSettingsCapturingSlot.captured.workingDirectory).isEqualTo(
            File(
                projectBaseDir
            )
        )
    }
}
