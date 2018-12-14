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
import com.google.container.tools.skaffold.run.ui.SkaffoldFilesComboBox
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.google.container.tools.test.UiTest
import com.intellij.mock.MockVirtualFile
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Unit tests for [SkaffoldFilesComboBox] */
class SkaffoldFilesComboBoxTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @MockK
    @TestService
    private lateinit var mockSkaffoldFileService: SkaffoldFileService

    private lateinit var skaffoldFilesComboBox: SkaffoldFilesComboBox

    @Before
    fun setUp() {
        skaffoldFilesComboBox = SkaffoldFilesComboBox()
    }

    @Test
    @UiTest
    fun `skaffold combo box for a project with no files has no elements and empty selection`() {
        val project = containerToolsRule.ideaProjectTestFixture.project
        every { mockSkaffoldFileService.findSkaffoldFiles(project) } returns listOf()
        skaffoldFilesComboBox.setProject(project)

        assertThat(skaffoldFilesComboBox.getSelectedSkaffoldFile()).isNull()
    }

    @Test
    @UiTest
    fun `single skaffold file from project is pre-selected in combo box`() {
        val project = containerToolsRule.ideaProjectTestFixture.project
        val mockSkaffoldFile = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(project) } returns
            listOf(mockSkaffoldFile)
        skaffoldFilesComboBox.setProject(project)

        assertThat(skaffoldFilesComboBox.getSelectedSkaffoldFile()).isEqualTo(mockSkaffoldFile)
    }

    @Test
    @UiTest
    fun `first skaffold file from multi-file project is pre-selected in combo box`() {
        val project = containerToolsRule.ideaProjectTestFixture.project
        val mockSkaffoldFile1 = MockVirtualFile.file("k8s/deploy.yaml")
        val mockSkaffoldFile2 = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(project) } returns listOf(
            mockSkaffoldFile1,
            mockSkaffoldFile2
        )
        skaffoldFilesComboBox.setProject(project)

        assertThat(skaffoldFilesComboBox.getSelectedSkaffoldFile()).isEqualTo(mockSkaffoldFile1)
    }

    @Test
    @UiTest
    fun `setSelectedSkaffoldFile with existing file changes selection in combo box`() {
        val project = containerToolsRule.ideaProjectTestFixture.project
        val mockSkaffoldFile1 = MockVirtualFile.file("k8s/deploy.yaml")
        val mockSkaffoldFile2 = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(project) } returns listOf(
            mockSkaffoldFile1,
            mockSkaffoldFile2
        )
        skaffoldFilesComboBox.setProject(project)
        skaffoldFilesComboBox.setSelectedSkaffoldFile(mockSkaffoldFile2)

        // make sure the file is not double-added.
        assertThat(skaffoldFilesComboBox.model.size).isEqualTo(2)
        assertThat(skaffoldFilesComboBox.getSelectedSkaffoldFile()).isEqualTo(mockSkaffoldFile2)
    }

    @Test
    @UiTest
    fun `setSelectedSkaffoldFile with non-existing file changes selection and adds element`() {
        val project = containerToolsRule.ideaProjectTestFixture.project
        val mockSkaffoldFile1 = MockVirtualFile.file("k8s/deploy.yaml")
        val mockSkaffoldFile2 = MockVirtualFile.file("skaffold.yaml")
        every { mockSkaffoldFileService.findSkaffoldFiles(project) } returns listOf(
            mockSkaffoldFile1,
            mockSkaffoldFile2
        )
        skaffoldFilesComboBox.setProject(project)
        // non-existing file from previously saved configuration case
        val nonExistingSkaffoldFile = MockVirtualFile.file("old-one.yaml")
        skaffoldFilesComboBox.setSelectedSkaffoldFile(nonExistingSkaffoldFile)

        // make sure the file is added
        assertThat(skaffoldFilesComboBox.model.size).isEqualTo(3)
        assertThat(skaffoldFilesComboBox.getSelectedSkaffoldFile()).isEqualTo(
            nonExistingSkaffoldFile
        )
    }
}
