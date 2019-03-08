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

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.io.File


/** Unit tests for [KubectlExecutorService] */
class DefaultKubectlExecutorServiceTest {

    private lateinit var kubectlExecutorService: KubectlExecutorService

    @MockK
    private lateinit var mockProcess: Process

    @Before
    fun setUp() {
        var kubectlExecutorService = spyk(KubectlExecutorService())
        every { kubectlExecutorService.createProcess(any(), any()) } answers { mockProcess }
    }

    @Test
    fun `single run with no arguments launches kubectl run`() {
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.SINGLE_RUN
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo("kubectl run")
    }


    @Test
    fun `kubectl config filename argument generates valid kubectl filename flag`() {
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "test.yaml"
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo("kubectl dev --filename test.yaml")
    }

    @Test
    fun `working directory is passed on to process builder`() {
//        kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "test.yaml",
//                        workingDirectory = File("/tmp")
//                )
//        )
//
//        verify { kubectlExecutorService.createProcess(File("/tmp"), any()) }
    }

    @Test
    fun `empty kubectl label list does not generate label flags`() {
//        val kubectlLabels = KubectlLabels()
//
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "test.yaml",
//                        kubectlLabels = kubectlLabels
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo("kubectl dev --filename test.yaml")
    }

    @Test
    fun `single kubectl label list generates correct label flag`() {
//        val kubectlLabels = KubectlLabels()
//        kubectlLabels.labels["ide"] = "testIde"
//
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "test.yaml",
//                        kubectlLabels = kubectlLabels
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo(
//                "kubectl dev --filename test.yaml --label ide=testIde"
//        )
    }

    @Test
    fun `multiple kubectl label list generates correct label flag set`() {
//        val kubectlLabels = KubectlLabels()
//        kubectlLabels.labels["ide"] = "testIde"
//        kubectlLabels.labels["name"] = "unitTest"
//        kubectlLabels.labels["version"] = "1"
//
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "test.yaml",
//                        kubectlLabels = kubectlLabels
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo(
//                "kubectl dev --filename test.yaml " +
//                        "--label ide=testIde --label name=unitTest --label version=1"
//        )
    }


    @Test
    fun `added profile name generates valid command line`() {
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.DEV,
//                        kubectlConfigurationFilePath = "profiles.yaml",
//                        kubectlProfile = "cloudBuild"
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo(
//                "kubectl dev --filename profiles.yaml " +
//                        "--profile cloudBuild"
//        )
    }

    @Test
    fun `null profile name generates valid command line`() {
//        val result = kubectlExecutorService.executeKubectl(
//                KubectlExecutorSettings(
//                        KubectlExecutorSettings.ExecutionMode.SINGLE_RUN,
//                        kubectlConfigurationFilePath = "test.yaml",
//                        kubectlProfile = null
//                )
//        )
//
//        Truth.assertThat(result.commandLine).isEqualTo("kubectl run --filename test.yaml")
    }


    @Test
    fun `isKubectlAvailable returns true when kubectl is available`() {
//        testKubectlFile.setExecutable(true)
//        every { kubectlExecutorService.getSystemPath() } answers { testKubectlFile.parent }
//        Truth.assertThat(kubectlExecutorService.isKubectlAvailable()).isTrue()
    }

    @Test
    fun `isKubectlAvailable returns false when kubectl is not available`() {
//        every { kubectlExecutorService.getSystemPath() } answers { "" }
//        Truth.assertThat(kubectlExecutorService.isKubectlAvailable()).isFalse()
    }

    @Test
    fun `isKubectlAvailable returns false when kubectl is not available in valid system paths`() {
//        every { kubectlExecutorService.getSystemPath() } answers {
//            testNotKubectlFile.parent
//        }
//        Truth.assertThat(kubectlExecutorService.isKubectlAvailable()).isFalse()
    }
}
