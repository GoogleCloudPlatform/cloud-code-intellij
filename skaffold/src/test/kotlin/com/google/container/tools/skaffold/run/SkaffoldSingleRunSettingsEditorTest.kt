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
import com.intellij.mock.MockVirtualFile
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import io.mockk.every
import io.mockk.impl.annotations.SpyK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Unit tests for [SkaffoldSingleRunSettingsEditor] */
class SkaffoldSingleRunSettingsEditorTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @SpyK
    @TestService
    private var mockSkaffoldFileService: SkaffoldFileService = SkaffoldFileService()

    private lateinit var skaffoldSingleRunConfiguration: SkaffoldSingleRunConfiguration

    private lateinit var singleRunSettingsEditor: SkaffoldSingleRunSettingsEditor

    private val skaffoldFile: MockVirtualFile
        get() {
            val skaffoldFile = MockVirtualFile.file("test.yaml")
            skaffoldFile.setText("apiVersion: skaffold/v1beta1")
            return skaffoldFile
        }

    @Before
    fun setUp() {
        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            singleRunSettingsEditor = SkaffoldSingleRunSettingsEditor()
            // calls getComponent() to initialize UI first, IDE flow.
            singleRunSettingsEditor.component
        })

        // default valid skaffold file for tests
        every { mockSkaffoldFileService.findSkaffoldFiles(any()) } returns listOf(skaffoldFile)

        // empty single run configuration
        skaffoldSingleRunConfiguration = SkaffoldSingleRunConfiguration(
            containerToolsRule.ideaProjectTestFixture.project,
            SkaffoldSingleRunConfigurationFactory(SkaffoldRunConfigurationType()),
            "test"
        )
    }

    @Test
    @UiTest
    fun `checked tail logs checkbox generates valid configuration`() {
        singleRunSettingsEditor.resetFrom(skaffoldSingleRunConfiguration)
        singleRunSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)
        singleRunSettingsEditor.tailLogsCheckbox.isSelected = true

        singleRunSettingsEditor.applyTo(skaffoldSingleRunConfiguration)

        assertThat(skaffoldSingleRunConfiguration.tailDeploymentLogs).isTrue()
    }

    @Test
    @UiTest
    fun `unchecked tail logs checkbox generates valid configuration`() {
        singleRunSettingsEditor.resetFrom(skaffoldSingleRunConfiguration)
        singleRunSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)
        singleRunSettingsEditor.tailLogsCheckbox.isSelected = false

        singleRunSettingsEditor.applyTo(skaffoldSingleRunConfiguration)

        assertThat(skaffoldSingleRunConfiguration.tailDeploymentLogs).isFalse()
    }

    @Test
    @UiTest
    fun `tail logs checkbox is unselected by default`() {
        singleRunSettingsEditor.resetFrom(skaffoldSingleRunConfiguration)
        singleRunSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)

        assertThat(singleRunSettingsEditor.tailLogsCheckbox.isSelected).isFalse()
    }

    @Test
    @UiTest
    fun `tail logs option is not enabled by default settings`() {
        singleRunSettingsEditor.resetFrom(skaffoldSingleRunConfiguration)
        singleRunSettingsEditor.skaffoldFilesComboBox.setSelectedSkaffoldFile(skaffoldFile)

        singleRunSettingsEditor.applyTo(skaffoldSingleRunConfiguration)

        assertThat(skaffoldSingleRunConfiguration.tailDeploymentLogs).isFalse()
    }
}
