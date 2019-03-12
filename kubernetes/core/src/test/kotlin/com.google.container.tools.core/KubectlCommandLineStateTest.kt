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

package com.google.container.tools.core

import com.google.common.truth.Truth.assertThat


import com.google.container.tools.core.util.CoreBundle
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestFile
import com.google.container.tools.test.TestService
import com.google.container.tools.test.expectThrows
import com.google.kubernetes.tools.core.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.SearchScopeProvider
import com.intellij.execution.runners.ExecutionEnvironment
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

/** Unit tests for [KubectlCommandLineState] */
class KubectlCommandLineStateTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var kubectlCommandLineState: KubectlCommandLineState

    @MockK
    private lateinit var mockExecutionEnvironment: ExecutionEnvironment
    @MockK
    private lateinit var mockRunnerSettings: RunnerAndConfigurationSettings
    @MockK
    private lateinit var mockKubectlRunConfig: KubectlRunConfiguration

    @TestFile(name = "myFile1", contents = "")
    private lateinit var myFile1: File

    @MockK
    @TestService
    private lateinit var mockKubectlExecutorService: KubectlExecutorService
    @MockK
    @TestService
    private lateinit var mockPluginInfoService: PluginInfo

    private val kubectlSettingsCapturingSlot: CapturingSlot<KubectlExecutorSettings> = slot()

    @Before
    fun setUp() {
        every {
            mockExecutionEnvironment.runnerAndConfigurationSettings
        } answers { mockRunnerSettings }
        every {mockRunnerSettings.configuration } answers { mockKubectlRunConfig}
        every { mockKubectlRunConfig.validConfigurationFlags() } answers { true }
        // pass project into the CLI state
        every { mockExecutionEnvironment.project } answers {
            containerToolsRule.ideaProjectTestFixture.project
        }
        every { mockKubectlExecutorService.isKubectlAvailable() } answers {true}

        // CommandLineState calls this static method, needs to be mocked
        mockkStatic(SearchScopeProvider::class)
        every { SearchScopeProvider.createSearchScope(any(), any()) } answers { mockk() }

        // Kubectl executor answer mocks
        val mockKubectlProcess: KubectlProcess = mockk(relaxed = true)
        every {
            mockKubectlExecutorService.executeKubectl(capture(kubectlSettingsCapturingSlot))
        } answers { mockKubectlProcess }
    }

    @Test
    fun `unsupported run configuration type throws execution exception`() {
        val invalidKubectlRunConfiguration = mockk<RunConfigurationBase<Element>>()
        every { mockRunnerSettings.configuration } answers { invalidKubectlRunConfiguration }
        kubectlCommandLineState = KubectlCommandLineState(
                mockExecutionEnvironment,
                KubectlExecutorSettings.ExecutionMode.VERSION
        )

        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable { kubectlCommandLineState.startProcess() })
        assertThat(exception.message).isEqualTo(
                CoreBundle.message("kubectl.corrupted.run.settings")
        )
    }


    @Test
    fun `given valid settings kubectl process is executed from the project directory`() {
        val projectBaseDir: String =
                containerToolsRule.ideaProjectTestFixture.project.guessProjectDir()!!.path


        every { mockKubectlRunConfig.configurationFlags} answers {arrayListOf("-f", myFile1.path)}

        kubectlCommandLineState = KubectlCommandLineState(
                mockExecutionEnvironment,
                KubectlExecutorSettings.ExecutionMode.CREATE
        )
        kubectlCommandLineState.startProcess()

        assertThat(kubectlSettingsCapturingSlot.captured.workingDirectory).isEqualTo(
                File(
                        projectBaseDir
                )
        )
    }

    @Test
    fun `A run error is thrown if kubectl is not in the system PATH`() {
        every { mockKubectlExecutorService.isKubectlAvailable() } answers { false }
        kubectlCommandLineState = KubectlCommandLineState(
                mockExecutionEnvironment,
                KubectlExecutorSettings.ExecutionMode.VERSION
        )

        expectThrows(
                ExecutionException::class,
                ThrowableRunnable { kubectlCommandLineState.startProcess() })
    }

    @Test
    fun `A run error is not thrown if kubectl is in the system PATH`() {

        kubectlCommandLineState = KubectlCommandLineState(
                mockExecutionEnvironment,
                KubectlExecutorSettings.ExecutionMode.VERSION
        )

        kubectlCommandLineState.startProcess()

        assertThat(kubectlSettingsCapturingSlot.captured.workingDirectory).isNotNull()
    }
}