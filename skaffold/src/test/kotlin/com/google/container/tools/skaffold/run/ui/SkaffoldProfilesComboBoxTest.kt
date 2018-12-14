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
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.UiTest
import com.intellij.mock.MockVirtualFile
import org.junit.Rule
import org.junit.Test

/** Unit tests for [SkaffoldProfilesComboBox] */
class SkaffoldProfilesComboBoxTest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @Test
    @UiTest
    fun `no skaffold yaml file results in empty disabled profiles combobox`() {
        val profilesComboBox = SkaffoldProfilesComboBox()

        assertThat(profilesComboBox.model.size).isEqualTo(0)
        assertThat(profilesComboBox.isEnabled).isFalse()
    }

    @Test
    @UiTest
    fun `no skaffold yaml file results in null profile selection`() {
        val profilesComboBox = SkaffoldProfilesComboBox()

        assertThat(profilesComboBox.getSelectedProfile()).isNull()
    }

    @Test
    @UiTest
    fun `skaffold yaml without profiles results in disabled combobox with default profile`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.model.size).isEqualTo(1)
        assertThat(profilesComboBox.model.getElementAt(0)).isEqualTo("default")
        assertThat(profilesComboBox.isEnabled).isFalse()
    }

    @Test
    @UiTest
    fun `skaffold yaml without profiles results in default profile selection`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.getSelectedProfile()).isNull()
    }

    @Test
    @UiTest
    fun `skaffold yaml with profiles produces valid list of profiles to select from`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.model.size).isEqualTo(3)
        assertThat(profilesComboBox.model.getElementAt(0)).isEqualTo("default")
        assertThat(profilesComboBox.model.getElementAt(1)).isEqualTo("localImage")
        assertThat(profilesComboBox.model.getElementAt(2)).isEqualTo("gcb")
        assertThat(profilesComboBox.isEnabled).isTrue()
    }

    @Test
    @UiTest
    fun `given skaffold yaml with profiles getSelectedProfile returns default profile`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.getSelectedProfile()).isNull()
    }

    @Test
    @UiTest
    fun `given list of profiles combobox allows selecting existing profile`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)
        profilesComboBox.setSelectedProfile("gcb")

        assertThat(profilesComboBox.getSelectedProfile()).isEqualTo("gcb")
    }

    @Test
    @UiTest
    fun `given list of profiles combobox does not allow selecting non-existing profile`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)
        profilesComboBox.setSelectedProfile("localImage")
        profilesComboBox.setSelectedProfile("does-not-exist")

        assertThat(profilesComboBox.getSelectedProfile()).isEqualTo("localImage")
    }

    @Test
    @UiTest
    fun `with list of profiles selectedProfile() returns null for unmodified profile selection`() {
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
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.getSelectedProfile()).isNull()
    }

    @Test
    @UiTest
    fun `malformed skaffold yaml results in disabled empty combobox`() {
        val skaffoldYamlFile = MockVirtualFile.file("skaffold.yaml")
        skaffoldYamlFile.setText(
            """
            `apiVersion~ skaffold/v1beta1
            kind: Config
        """
        )
        val profilesComboBox = SkaffoldProfilesComboBox()

        profilesComboBox.skaffoldFileUpdated(skaffoldYamlFile)

        assertThat(profilesComboBox.model.size).isEqualTo(0)
        assertThat(profilesComboBox.isEnabled).isFalse()
    }
}
