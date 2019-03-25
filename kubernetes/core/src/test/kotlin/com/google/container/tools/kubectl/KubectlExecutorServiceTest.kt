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

package com.google.container.tools.kubectl

import com.google.common.truth.Truth.assertThat
import com.google.kubernetes.tools.test.ContainerToolsRule
import com.google.kubernetes.tools.test.TestFile
import com.google.kubernetes.tools.core.kubectl.KubectlExecutorService
import com.google.kubernetes.tools.core.kubectl.KubectlExecutorSettings
import com.google.kubernetes.tools.core.kubectl.KubectlProcess
import com.google.kubernetes.tools.core.util.CoreBundle.message
import com.google.kubernetes.tools.test.expectThrows
import com.intellij.execution.ExecutionException
import com.intellij.util.ThrowableRunnable
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/** Unit tests for [KubectlExecutorService] */
class DefaultKubectlExecutorServiceTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var kubectlExecutorService: KubectlExecutorService

    @MockK
    private lateinit var mockKubectlProcess: KubectlProcess

    @MockK
    private lateinit var mockProcess: Process

    @TestFile(name = "myFile1", contents = "")
    private lateinit var myFile1: File

    @TestFile(name = "myFile2", contents = "")
    private lateinit var myFile2: File

    @Before
    fun setUp() {
        kubectlExecutorService = spyk(KubectlExecutorService())
    }

    @Test
    fun `kubectl create filename argument generates valid kubectl filename flag`() {

        val result = kubectlExecutorService.concatArgs(
                KubectlExecutorSettings(
                        KubectlExecutorSettings.ExecutionMode.CREATE,
                        executionFlags = arrayListOf("-f", "./my1.yaml", "-f", "./my2.yaml")
                )
        )
        assertThat(result).isEqualTo(
                "kubectl create -f ./my1.yaml -f ./my2.yaml" )
    }

    @Test
    fun `isKubectlAvailable returns true when kubectl is available`() {
        every { kubectlExecutorService.runKubectlCommand(any()) } answers { mockProcess }
        every { mockProcess.exitValue() } answers { 0 }
        assertThat(kubectlExecutorService.isKubectlAvailable()).isTrue()
    }

    @Test
    fun `isKubectlAvailable returns false when kubectl is not available`() {
        every { kubectlExecutorService.runKubectlCommand(any()) } answers { mockProcess }
        every { mockProcess.exitValue() } answers { 1 }
        assertThat(kubectlExecutorService.isKubectlAvailable()).isFalse()
    }

    @Test
    fun `processOutputToString converts the output of a process to a string`() {
        val processResult = "version 1.2.3"
        every { mockProcess.inputStream } answers {
            processResult.byteInputStream(StandardCharsets.UTF_8)
        }
        assertThat(kubectlExecutorService.processOutputToString(mockProcess))
                .isEqualTo("version 1.2.3")
    }

    @Test
    fun `getKubectlOutputBlocking throws an exception if Kubectl isn't available`() {
        every { kubectlExecutorService.isKubectlAvailable() } answers { false }
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable {
                    kubectlExecutorService.getKubectlOutputBlocking(
                            KubectlExecutorSettings(
                                KubectlExecutorSettings.ExecutionMode.VERSION,
                                listOf()
                            )
                    )
                })
        assertThat(exception.message).isEqualTo(message("kubectl.not.on.system.error"))
    }

    @Test
    fun `getKubectlOutputBlocking throws an exception if invalid flags are supplied`() {
        every { kubectlExecutorService.isKubectlAvailable() } answers { true }
        every { kubectlExecutorService.runKubectlCommand(any()) } answers { mockProcess }
        every { mockProcess.exitValue() } answers { 1 }
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable {
                    kubectlExecutorService.getKubectlOutputBlocking(
                            KubectlExecutorSettings(
                                    KubectlExecutorSettings.ExecutionMode.VERSION,
                                    listOf("-f", myFile1.path, "-f", myFile2.path)
                            )
                    )
                })
        assertThat(exception.message).isEqualTo(message("kubectl.unknown.error"))
    }

    @Test
    fun `getKubectlOutputBlocking throws an exception if the process exitValue isn't 0`() {
        every { kubectlExecutorService.isKubectlAvailable() } answers { true }
        every { kubectlExecutorService.runKubectlCommand(any()) } answers { mockProcess }
        every { mockProcess.exitValue() } answers { 1 }
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable {
                    kubectlExecutorService.getKubectlOutputBlocking(
                            KubectlExecutorSettings(
                                    KubectlExecutorSettings.ExecutionMode.VERSION,
                                    listOf()
                            )
                    )
                })
        assertThat(exception.message).isEqualTo(message("kubectl.unknown.error"))
    }

    @Test
    fun `getKubectlOutputBlocking returns string output with valid inputs`() {
        val expectedResponse = "this is the version"
        every { kubectlExecutorService.isKubectlAvailable() } answers { true }
        every { kubectlExecutorService.runKubectlCommand(any()) } answers { mockProcess }
        every { mockProcess.exitValue() } answers { 0 }
        every { kubectlExecutorService.processOutputToString(any()) } answers { expectedResponse }
        val response = kubectlExecutorService.getKubectlOutputBlocking(
                KubectlExecutorSettings(
                    KubectlExecutorSettings.ExecutionMode.VERSION,
                    listOf()
                )
        )
        assertThat(response).isEqualTo(expectedResponse)
    }
}
