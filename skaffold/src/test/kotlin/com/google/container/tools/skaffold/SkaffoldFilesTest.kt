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

package com.google.container.tools.skaffold

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.test.ContainerToolsRule
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.application.ApplicationManager
import org.junit.Rule
import org.junit.Test

/** Unit tests for Skaffold files functionality in [SkaffoldFiles.kt] */
class SkaffoldFilesTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule()

    @Test
    fun `valid skaffold file is accepted`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldFile.setText("apiVersion: skaffold/v1alpha2")

        assertThat(isSkaffoldFile(skaffoldFile)).isTrue()
    }

    @Test
    fun `valid skaffold file not named skaffold_yaml is accepted`() {
        val skaffoldFile = MockVirtualFile.file("tests-deploys.yaml")
        skaffoldFile.setText("apiVersion: skaffold/v1alpha3")

        assertThat(isSkaffoldFile(skaffoldFile)).isTrue()
    }

    @Test
    fun `extra spaces and tabs in header skaffold file is accepted`() {
        val skaffoldFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldFile.setText(
            """ apiVersion:    skaffold/v1alpha3
                  kind: Config
                  build:

            """
        )

        assertThat(isSkaffoldFile(skaffoldFile)).isTrue()
    }

    @Test
    fun `kubernetes file is not valid skaffold file`() {
        val k8sFile = MockVirtualFile.file("deploy.yaml")
        k8sFile.setText("apiVersion: apps/v1")

        assertThat(isSkaffoldFile(k8sFile)).isFalse()
    }

    @Test
    fun `empty IDE project does not contain skaffold yaml files`() {
        val project = containerToolsRule.ideaProjectTestFixture.project

        ApplicationManager.getApplication().runReadAction {
            val skaffoldFiles = findSkaffoldFiles(project)
            assertThat(skaffoldFiles).isEmpty()
        }
    }
}
