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

package com.google.container.tools.skaffold.run.ui

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.skaffold.run.AbstractSkaffoldRunConfiguration
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

    private lateinit var baseSkaffoldSettingsEditor:
        BaseSkaffoldSettingsEditor<AbstractSkaffoldRunConfiguration>

    @Before
    fun setUp() {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            baseSkaffoldSettingsEditor =
                BaseSkaffoldSettingsEditor("test")
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

        // capture settings update
        every {
            mockSkaffoldSettings setProperty "skaffoldConfigurationFilePath" value any<String>()
        } propertyType String::class answers {
            assertThat(value).isEqualTo(skaffoldFile.path)
        }

        baseSkaffoldSettingsEditor.applyTo(mockSkaffoldSettings)
    }

    @Test
    @UiTest
    fun `given valid Skaffold configuration resetFrom selects saved file name in file combobox`() {
        val skaffoldFile = MockVirtualFile.file("test.yaml")
        skaffoldFile.setText("apiVersion: skaffold/v1beta1")
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf(skaffoldFile)
        every { mockSkaffoldFileService.isSkaffoldFile(skaffoldFile) } returns true
        every {
            mockSkaffoldSettings.skaffoldConfigurationFilePath
        } answers { skaffoldFile.path }

        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)

        assertThat(
            baseSkaffoldSettingsEditor.skaffoldFilesComboBox.getSelectedSkaffoldFile()
        )
            .isEqualTo(skaffoldFile)
    }

    @Test
    @UiTest
    fun `given valid Skaffold configuration with profiles applyTo successfully saves settings`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldFile.setText(
            """
            apiVersion: skaffold/v1beta1
            kind: Config
            profiles:
              - name: gcb
                build:
                  googleCloudBuild:
                    projectId: k8s-skaffold
        """
        )
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf(skaffoldFile)
        every { mockSkaffoldFileService.isSkaffoldFile(skaffoldFile) } returns true
        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)
        baseSkaffoldSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)
        baseSkaffoldSettingsEditor.skaffoldProfilesComboBox.setSelectedProfile("gcb")

        // capture settings update
        every {
            mockSkaffoldSettings setProperty "skaffoldProfile" value any<String>()
        } propertyType String::class answers {
            assertThat(value).isEqualTo("gcb")
        }

        baseSkaffoldSettingsEditor.applyTo(mockSkaffoldSettings)
    }

    @Test
    @UiTest
    fun `given valid Skaffold profiles resetFrom selects saved profile name in combobox`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldFile.setText(
            """
            apiVersion: skaffold/v1beta1
            kind: Config
            profiles:
              - name: gcb
                build:
                  googleCloudBuild:
                    projectId: k8s-skaffold
        """
        )
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf(skaffoldFile)
        every { mockSkaffoldFileService.isSkaffoldFile(skaffoldFile) } returns true
        every {
            mockSkaffoldSettings.skaffoldConfigurationFilePath
        } answers { skaffoldFile.path }
        every {
            mockSkaffoldSettings.skaffoldProfile
        } answers { "gcb" }

        baseSkaffoldSettingsEditor.resetFrom(mockSkaffoldSettings)

        assertThat(baseSkaffoldSettingsEditor.skaffoldProfilesComboBox.getSelectedProfile())
            .isEqualTo("gcb")
    }
}
