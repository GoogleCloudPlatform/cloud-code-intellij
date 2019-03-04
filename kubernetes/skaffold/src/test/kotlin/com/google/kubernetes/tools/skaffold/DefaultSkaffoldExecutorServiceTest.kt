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
import com.google.kubernetes.tools.core.settings.KubernetesSettingsService
import com.google.kubernetes.tools.test.ContainerToolsRule
import com.google.kubernetes.tools.test.TestFile
import com.google.kubernetes.tools.test.TestService
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

    @TestFile(name = "skaffold", contents = "Some contents")
    private lateinit var testSkaffoldFile: File

    @TestFile(name = "notSkaffold", contents = "Some contents")
    private lateinit var testNotSkaffoldFile: File

    @TestService
    @MockK
    private lateinit var kubernetesSettingsService: KubernetesSettingsService

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

    @Test
    fun `empty skaffold label list does not generate label flags`() {
        val skaffoldLabels = SkaffoldLabels()

        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml",
                skaffoldLabels = skaffoldLabels
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev --filename test.yaml")
    }

    @Test
    fun `single skaffold label list generates correct label flag`() {
        val skaffoldLabels = SkaffoldLabels()
        skaffoldLabels.labels["ide"] = "testIde"

        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml",
                skaffoldLabels = skaffoldLabels
            )
        )

        assertThat(result.commandLine).isEqualTo(
            "skaffold dev --filename test.yaml --label ide=testIde"
        )
    }

    @Test
    fun `multiple skaffold label list generates correct label flag set`() {
        val skaffoldLabels = SkaffoldLabels()
        skaffoldLabels.labels["ide"] = "testIde"
        skaffoldLabels.labels["name"] = "unitTest"
        skaffoldLabels.labels["version"] = "1"

        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml",
                skaffoldLabels = skaffoldLabels
            )
        )

        assertThat(result.commandLine).isEqualTo(
            "skaffold dev --filename test.yaml " +
                "--label ide=testIde --label name=unitTest --label version=1"
        )
    }

    @Test
    fun `tail logs option set to true generates valid command line`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml",
                tailLogsAfterDeploy = true
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev --filename test.yaml --tail")
    }

    @Test
    fun `tail logs option set to false does not add --tail option`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "skaffold.yaml",
                tailLogsAfterDeploy = false
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev --filename skaffold.yaml")
    }

    @Test
    fun `added profile name generates valid command line`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "profiles.yaml",
                skaffoldProfile = "cloudBuild"
            )
        )

        assertThat(result.commandLine).isEqualTo(
            "skaffold dev --filename profiles.yaml " +
                "--profile cloudBuild"
        )
    }

    @Test
    fun `null profile name generates valid command line`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.SINGLE_RUN,
                skaffoldConfigurationFilePath = "test.yaml",
                skaffoldProfile = null
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold run --filename test.yaml")
    }

    @Test
    fun `added default image repo name generates valid command line`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "skaffold.yaml",
                defaultImageRepo = "gcr.io/k8s-tests"
            )
        )

        assertThat(result.commandLine).isEqualTo(
            "skaffold dev --filename skaffold.yaml " +
                "--default-repo gcr.io/k8s-tests"
        )
    }

    @Test
    fun `null default image repo name generates valid command line without repo override`() {
        val result = defaultSkaffoldExecutorService.executeSkaffold(
            SkaffoldExecutorSettings(
                SkaffoldExecutorSettings.ExecutionMode.DEV,
                skaffoldConfigurationFilePath = "test.yaml"
            )
        )

        assertThat(result.commandLine).isEqualTo("skaffold dev --filename test.yaml")
    }

    @Test
    fun `isSkaffoldAvailable returns true when skaffold is on the PATH only`() {
        testSkaffoldFile.setExecutable(true)
        every { defaultSkaffoldExecutorService.getSystemPath() } answers { testSkaffoldFile.parent }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isTrue()
    }

    @Test
    fun `isSkaffoldAvailable returns false when skaffold is not on the PATH or in settings`() {
        every { defaultSkaffoldExecutorService.getSystemPath() } answers { "" }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isFalse()
    }

    @Test
    fun `isSkaffoldAvailable returns false when skaffold is not available in valid system paths`() {
        every { defaultSkaffoldExecutorService.getSystemPath() } answers {
            testNotSkaffoldFile.parent
        }
        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isFalse()
    }

    @Test
    fun `isSkaffoldAvailable returns true when skaffold is configured in the settings only`() {
        every { defaultSkaffoldExecutorService.getSystemPath() } answers {
            testNotSkaffoldFile.parent
        }
        every { kubernetesSettingsService.skaffoldExecutablePath } answers { "/path/to/skaffold" }

        assertThat(defaultSkaffoldExecutorService.isSkaffoldAvailable()).isTrue()
    }
}
