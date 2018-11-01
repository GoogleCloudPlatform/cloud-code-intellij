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
import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.google.container.tools.test.UiTest
import com.google.container.tools.test.expectThrows
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for base Skaffold settings support defined in [BaseSkaffoldSettingsEditor] and
 * [AbstractSkaffoldRunConfiguration].
 */
class BaseSkaffoldSettingsEditorTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @SpyK
    @TestService
    private var mockSkaffoldFileService: SkaffoldFileService = SkaffoldFileService()

    @MockK
    private lateinit var mockSkaffoldSettings: AbstractSkaffoldRunConfiguration

    private lateinit var baseSkaffoldSettingsEditor: BaseSkaffoldSettingsEditor

    @Before
    fun setUp() {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            baseSkaffoldSettingsEditor = BaseSkaffoldSettingsEditor("test")
            // calls getComponent() to initialize UI first, IDE flow.
            baseSkaffoldSettingsEditor.component
        })

        // default is project with no Skaffold files
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf()
        // return test fixture project for settings
        every { mockSkaffoldSettings.project } answers
            { containerToolsRule.ideaProjectTestFixture.project }
    }

    @Test
    @UiTest
    fun `when project has no Skaffold files resetFrom throws a ConfigurationException`() {
        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)

        expectThrows(
            ConfigurationException::class,
            ThrowableRunnable { baseSkaffoldSettingsEditor.applyTo(mockSkaffoldSettings) })
    }

    @Test
    @UiTest
    fun `given non-existing Skaffold file resetFrom throws a ConfigurationException`() {
        every { mockSkaffoldSettings.skaffoldConfigurationFilePath } answers { "no-such-file.yaml" }
        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)

        expectThrows(
            ConfigurationException::class,
            ThrowableRunnable { baseSkaffoldSettingsEditor.applyTo(mockSkaffoldSettings) })
    }

    @Test
    @UiTest
    fun `given valid Skaffold configuration file applyTo successfully saves settings`() {
        val skaffoldFile = MockVirtualFile.file("tests-deploy.yaml")
        skaffoldFile.setText("apiVersion: skaffold/v1alpha3")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf(skaffoldFile)
        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)
        baseSkaffoldSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)
        baseSkaffoldSettingsEditor.applyTo(mockSkaffoldSettings)

        // capture settings update
        every {
            mockSkaffoldSettings setProperty "skaffoldConfigurationFilePath" value any<String>()
        } propertyType String::class answers {
            fieldValue = value
            assertThat(value).isEqualTo(skaffoldFile.path)
        }
    }
}
