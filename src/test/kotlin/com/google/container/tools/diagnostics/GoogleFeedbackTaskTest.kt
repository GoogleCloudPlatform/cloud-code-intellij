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

package com.google.container.tools.diagnostics

import com.google.container.tools.diagnostics.GoogleFeedbackTask.FeedbackSender
import com.google.container.tools.test.ContainerToolsRule
import com.intellij.openapi.progress.ProgressIndicator
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Tests for [GoogleFeedbackTask].
 */
class GoogleFeedbackTaskTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var feedbackTask: GoogleFeedbackTask

    private val cause = Throwable("cause")
    private val keyValues = mapOf("key" to "value")
    private val errorMesssage = "test message"
    private val errorDescription = "test description"
    private val version = "test version"

    @MockK
    private lateinit var resultConsummer: (String) -> Unit
    @MockK
    private lateinit var errorCallback: (Exception) -> Unit
    @MockK
    private lateinit var feedbackSender: FeedbackSender
    @MockK
    private lateinit var progressIndicator: ProgressIndicator

    @Before
    fun setUp() {
        feedbackTask = GoogleFeedbackTask(
            containerToolsRule.ideaProjectTestFixture.project,
            "test title",
            true,
            cause,
            keyValues,
            errorMesssage,
            errorDescription,
            version,
            resultConsummer,
            errorCallback,
            feedbackSender
        )
    }

    @Test
    fun run_withValidResult_consumesResult() {
        val result = "result"
        every {
            feedbackSender.sendFeedback(
                GoogleFeedbackTask.CONTAINER_TOOLS_PRODUCT,
                GoogleFeedbackTask.CONTAINER_TOOLS_PACKAGE_NAME,
                cause,
                errorMesssage,
                errorDescription,
                version,
                keyValues
            )
        } returns result

        feedbackTask.run(progressIndicator)

        verify(exactly = 1) { resultConsummer(result) }
        verify(exactly = 0) { errorCallback(any()) }
    }

    @Test
    fun run_withException_consumesException() {
        val exception = IOException("test exception")
        every {
            feedbackSender.sendFeedback(
                GoogleFeedbackTask.CONTAINER_TOOLS_PRODUCT,
                GoogleFeedbackTask.CONTAINER_TOOLS_PACKAGE_NAME,
                cause,
                errorMesssage,
                errorDescription,
                version,
                keyValues
            )
        } throws exception

        feedbackTask.run(progressIndicator)

        verify(exactly = 0) { resultConsummer(any()) }
        verify(exactly = 1) { errorCallback(exception) }
    }
}
