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
import com.google.container.tools.skaffold.run.SkaffoldDevConfiguration
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.mock.MockVirtualFile
import com.intellij.notification.Notification
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Unit tests for [SkaffoldConfigurationDetector] */
class SkaffoldConfigurationDetectorTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @MockK
    @TestService
    private lateinit var mockSkaffoldFileService: SkaffoldFileService

    @MockK
    @TestService
    private lateinit var mockRunManager: RunManager

    private val runManagerSettingsCapture: CapturingSlot<RunnerAndConfigurationSettings> = slot()

    private lateinit var skaffoldConfigurationDetector: SkaffoldConfigurationDetector

    @MockK
    private lateinit var mockSkaffoldDevConfiguration: SkaffoldDevConfiguration

    @MockK
    private lateinit var mockNotification: Notification

    @Before
    fun setUp() {
        skaffoldConfigurationDetector = spyk(
            SkaffoldConfigurationDetector(containerToolsRule.ideaProjectTestFixture.project)
        )

        every {
            skaffoldConfigurationDetector.createNotification(
                any(),
                any(),
                any()
            )
        } answers { mockNotification }

        every { skaffoldConfigurationDetector.getRunManager(any()) } answers { mockRunManager }
        every {
            skaffoldConfigurationDetector.findConfigurationFactoryById(any())
        } answers { mockk() }
        every { mockRunManager.addConfiguration(capture(runManagerSettingsCapture)) } returns Unit
    }

    @Test
    fun `project with no skaffold files does not show prompt to add run configs`() {
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } answers { listOf() }

        skaffoldConfigurationDetector.projectOpened()

        verify(exactly = 0) { skaffoldConfigurationDetector.createNotification(any(), any()) }
    }

    @Test
    fun `project with skaffold files and no skaffold config prompts to add run configs`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } answers { listOf(skaffoldFile) }

        skaffoldConfigurationDetector.projectOpened()

        verify(exactly = 1) { skaffoldConfigurationDetector.createNotification(any(), any()) }
    }

    @Test
    fun `project with skaffold files and existing skaffold config doesnt ask to add configs`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } answers { listOf(skaffoldFile) }
        every { mockRunManager.allConfigurationsList } returns listOf(mockSkaffoldDevConfiguration)

        skaffoldConfigurationDetector.projectOpened()

        verify(exactly = 0) { skaffoldConfigurationDetector.createNotification(any(), any()) }
    }

    @Test
    fun `add dev config action creates and adds valid skaffold dev config`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } answers { listOf(skaffoldFile) }
        // capture and create requested single run configuration
        val nameSlot = slot<String>()
        val mockRunnerAndConfigurationSettings = mockk<RunnerAndConfigurationSettings>()
        every {
            mockRunManager.createConfiguration(
                capture(nameSlot),
                any<ConfigurationFactory>()
            )
        } answers { mockRunnerAndConfigurationSettings }

        every { mockRunnerAndConfigurationSettings.getConfiguration() } answers {
            mockSkaffoldDevConfiguration
        }

        skaffoldConfigurationDetector.addSkaffoldDevConfiguration(skaffoldFile.path)

        assertThat(nameSlot.captured).isEqualTo("develop on Kubernetes")
    }

    @Test
    fun `add run config action creates and adds valid skaffold run config`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } answers { listOf(skaffoldFile) }
        // capture and create requested single run configuration
        val nameSlot = slot<String>()
        val mockRunnerAndConfigurationSettings = mockk<RunnerAndConfigurationSettings>()
        every {
            mockRunManager.createConfiguration(
                capture(nameSlot),
                any<ConfigurationFactory>()
            )
        } answers { mockRunnerAndConfigurationSettings }

        every { mockRunnerAndConfigurationSettings.getConfiguration() } answers {
            mockSkaffoldDevConfiguration
        }

        skaffoldConfigurationDetector.addSkaffoldRunConfiguration(skaffoldFile.path)

        assertThat(nameSlot.captured).isEqualTo("deploy to Kubernetes")
    }
}
