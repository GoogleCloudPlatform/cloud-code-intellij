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

import com.google.common.truth.Truth
import com.google.kubernetes.tools.test.ContainerToolsRule
import com.google.kubernetes.tools.test.TestFile
import com.google.kubernetes.tools.kubectl.KubectlExecutorService
import com.google.kubernetes.tools.kubectl.KubectlExecutorSettings
import com.google.kubernetes.tools.kubectl.KubectlProcess
import com.google.kubernetes.tools.kubectl.message
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

import java.util.concurrent.TimeUnit


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
    fun `single run with no arguments launches kubectl config`() {
        val result = kubectlExecutorService.executeKubectl(
                KubectlExecutorSettings(
                        KubectlExecutorSettings.ExecutionMode.CONFIG
                )
        )
        Truth.assertThat(result.commandLine).isEqualTo("kubectl config")
    }


    @Test
    fun `kubectl create filename argument generates valid kubectl filename flag`() {
        val result = kubectlExecutorService.executeKubectl(
                KubectlExecutorSettings(
                        KubectlExecutorSettings.ExecutionMode.CREATE,
                        executionFlags = arrayListOf("-f", "./my1.yaml", "-f", "./my2.yaml")
                )
        )

        Truth.assertThat(result.commandLine).isEqualTo("kubectl create -f ./my1.yaml -f ./my2.yaml" )
    }

    @Test
    fun `kubectl create fake filename argument executes with exit value of 1`() {
        val result = kubectlExecutorService.executeKubectl(
                KubectlExecutorSettings(
                        KubectlExecutorSettings.ExecutionMode.CREATE,
                        executionFlags = arrayListOf("-f", myFile1.path, "-f", myFile2.path)
                )
        )
        result.process.waitFor(2, TimeUnit.SECONDS)
        Truth.assertThat(result.process.exitValue()).isEqualTo(1 )
    }


    @Test
    fun `kubectl invalid create arguments throws error`() {
        val result = kubectlExecutorService.executeKubectl(
                KubectlExecutorSettings(
                        KubectlExecutorSettings.ExecutionMode.VERSION,
                        executionFlags = arrayListOf("-f", myFile1.path, "-f", myFile2.path)
                )
        )
        result.process.waitFor(2, TimeUnit.SECONDS)
        Truth.assertThat(result.process.exitValue()).isEqualTo(1 )
    }

    @Test
    fun `isKubectlAvailable returns true when kubectl is available`() {
        every{ kubectlExecutorService.executeKubectl(any())} answers {mockKubectlProcess}
        every{ mockKubectlProcess.process.exitValue()} answers {0}
        Truth.assertThat(kubectlExecutorService.isKubectlAvailable()).isTrue()
    }

    @Test
    fun `isKubectlAvailable returns false when kubectl is not available`() {
        every{ kubectlExecutorService.executeKubectl(any())} answers {mockKubectlProcess}
        every{ mockKubectlProcess.process.exitValue()} answers {1}
        Truth.assertThat(kubectlExecutorService.isKubectlAvailable()).isFalse()
    }

    @Test
    fun `processOutputToString converts the output of a process to a string`(){
        val processResult = "version 1.2.3"
        every {mockProcess.inputStream} answers {
            processResult.byteInputStream(StandardCharsets.UTF_8)
        }
        Truth.assertThat(kubectlExecutorService.processOutputToString(mockProcess)).isEqualTo("version 1.2.3")
    }


    @Test
    fun `startProcess throws an exception if Kubectl isn't available`(){
        every{ kubectlExecutorService.isKubectlAvailable()} answers {false}
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable { kubectlExecutorService.startProcess(KubectlExecutorSettings.ExecutionMode.VERSION, listOf()) })
        Truth.assertThat(exception.message).isEqualTo(message("kubectl.not.on.system.error"))
    }

    @Test
    fun `startProcess throws an exception if invalid flags are supplied`(){
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable { kubectlExecutorService.startProcess(KubectlExecutorSettings.ExecutionMode.VERSION, listOf("-f", myFile1.path, "-f", myFile2.path)) })
        Truth.assertThat(exception.message).isEqualTo(message("kubectl.invalid.flags.error"))
    }

    @Test
    fun `startProcess throws an exception if the process exitValue isn't 0`(){
        every{ kubectlExecutorService.isKubectlAvailable()} answers {true}
        every{ kubectlExecutorService.executeKubectl(any())} answers {mockKubectlProcess}
        every{ mockKubectlProcess.process.exitValue()} answers {1}
        val exception = expectThrows(
                ExecutionException::class,
                ThrowableRunnable { kubectlExecutorService.startProcess(KubectlExecutorSettings.ExecutionMode.VERSION, listOf())})
        Truth.assertThat(exception.message).isEqualTo(message("kubectl.unknown.error"))
    }

    @Test
    fun `startProcess returns string output with valid inputs`(){
        val expectedResponse = "this is the version"
        every{ kubectlExecutorService.isKubectlAvailable()} answers {true}
        every{ kubectlExecutorService.executeKubectl(any())} answers {mockKubectlProcess}
        every{ mockKubectlProcess.process.exitValue()} answers {0}
        every{ kubectlExecutorService.processOutputToString(any())} answers {expectedResponse}
        val response = kubectlExecutorService.startProcess(KubectlExecutorSettings.ExecutionMode.VERSION, listOf())
        Truth.assertThat(response).isEqualTo(expectedResponse)
    }


}
