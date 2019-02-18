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
import com.google.container.tools.test.expectThrows
import com.intellij.mock.MockVirtualFile
import com.intellij.util.ThrowableRunnable
import org.junit.Test

/** Unit tests for [SkaffoldYamlConfiguration] */
class SkaffoldYamlConfigurationTest {

    @Test
    fun `valid skaffold yaml with no profiles correctly loads and maps to empty profile set`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText(
            """
            apiVersion: skaffold/v1beta1
            kind: Config
            build:
                artifact:
                - image: docker.io/local-image

        """
        )

        val skaffoldYamlConfiguration = SkaffoldYamlConfiguration(skaffoldYamlFile)

        assertThat(skaffoldYamlConfiguration.profiles).isEmpty()
    }

    @Test
    fun `single skaffold yaml profile correctly loads and maps to profile set`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText(
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

        val skaffoldYamlConfiguration = SkaffoldYamlConfiguration(skaffoldYamlFile)

        assertThat(skaffoldYamlConfiguration.profiles).isNotEmpty()
        assertThat(skaffoldYamlConfiguration.profiles.keys).isEqualTo(setOf("gcb"))
    }

    @Test
    fun `many skaffold yaml profiles correctly load and map to profile set`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText(
            """
            apiVersion: skaffold/v1beta1
            kind: Config
            profiles:
              - name: localImage
                build:
                  artifact:
                  - image: docker.io/local-image
              - name: gcb
                build:
                  googleCloudBuild:
                    projectId: k8s-skaffold
        """
        )

        val skaffoldYamlConfiguration = SkaffoldYamlConfiguration(skaffoldYamlFile)

        assertThat(skaffoldYamlConfiguration.profiles).isNotEmpty()
        assertThat(skaffoldYamlConfiguration.profiles.keys).isEqualTo(setOf("localImage", "gcb"))
    }

    @Test
    fun `invalid yaml file results in an exception`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText(
            """
            `apiVersion~ skaffold/v1beta1
            kind: Config
        """
        )

        expectThrows(
            Exception::class,
            ThrowableRunnable { SkaffoldYamlConfiguration(skaffoldYamlFile) })
    }

    @Test
    fun `empty yaml file results in an exception`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText("")

        expectThrows(
            Exception::class,
            ThrowableRunnable { SkaffoldYamlConfiguration(skaffoldYamlFile) })
    }
}
