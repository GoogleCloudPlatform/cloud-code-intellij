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

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.test.ContainerToolsRule
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.ex.ApplicationInfoEx
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [GoogleFeedbackErrorReporter].
 */
class GoogleFeedbackErrorReporterTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private val message = "test-message"
    private val description = "test-description"
    private val lastActionId = "test-last-action-id"
    private val fullProductName = "test-full-product-name"
    private val packageCode = "test-package-code"
    private val appVersionName = "test-app-version-name"
    private val isEap = true
    private val isInternal = true
    private val majorVersion = "test-major-version"
    private val minorVersion = "test-minor-version"

    @MockK
    lateinit var event: IdeaLoggingEvent
    @MockK
    lateinit var appNamesInfo: ApplicationNamesInfo
    @MockK
    lateinit var appInfoEx: ApplicationInfoEx
    @MockK
    lateinit var application: Application

    @Before
    fun setUp() {
        every { event.message } returns message
        every { appNamesInfo.fullProductName } returns fullProductName
        every { appInfoEx.packageCode } returns packageCode
        every { appInfoEx.versionName } returns appVersionName
        every { appInfoEx.isEAP } returns isEap
        every { application.isInternal } returns isInternal
        every { appInfoEx.majorVersion } returns majorVersion
        every { appInfoEx.minorVersion } returns minorVersion
    }

    @Test
    fun buildKeyValuesMap_returnsPopulatedMap() {
        val result: Map<String, String> = GoogleFeedbackErrorReporter().buildKeyValuesMap(
            event,
            description,
            lastActionId,
            appNamesInfo,
            appInfoEx,
            application
        )

        assertThat(result[GoogleFeedbackErrorReporter.ERROR_MESSAGE_KEY]).isEqualTo(message)
        assertThat(result[GoogleFeedbackErrorReporter.ERROR_DESCRIPTION_KEY])
            .isEqualTo(description)
        assertThat(result[GoogleFeedbackErrorReporter.LAST_ACTION_KEY]).isEqualTo(lastActionId)
        assertThat(result[GoogleFeedbackErrorReporter.APP_NAME_KEY]).isEqualTo(fullProductName)
        assertThat(result[GoogleFeedbackErrorReporter.APP_CODE_KEY]).isEqualTo(packageCode)
        assertThat(result[GoogleFeedbackErrorReporter.APP_NAME_VERSION_KEY])
            .isEqualTo(appVersionName)
        assertThat(result[GoogleFeedbackErrorReporter.APP_EAP_KEY]).isEqualTo(isEap.toString())
        assertThat(result[GoogleFeedbackErrorReporter.APP_INTERNAL_KEY])
            .isEqualTo(isInternal.toString())
        assertThat(result[GoogleFeedbackErrorReporter.APP_VERSION_MAJOR_KEY])
            .isEqualTo(majorVersion)
        assertThat(result[GoogleFeedbackErrorReporter.APP_VERSION_MINOR_KEY])
            .isEqualTo(minorVersion)
    }
}
