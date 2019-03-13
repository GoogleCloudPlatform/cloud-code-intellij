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

package com.google.kubernetes.tools.settings

import com.google.common.annotations.VisibleForTesting
import com.google.kubernetes.tools.core.settings.KubernetesSettingsService
import com.google.kubernetes.tools.core.util.CoreBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.layout.panel
import java.awt.Color
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Creates a "Kubernetes" menu item under the "Google" menu item in the IDE Settings.
 */
class KubernetesSettingsConfigurable : Configurable {

    @VisibleForTesting
    val skaffoldNotExecutableWarning =
            JLabel(CoreBundle.message(
                    "kubernetes.settings.dependencies.skaffold.not.executable.warning"))

    @VisibleForTesting
    val skaffoldBrowser = TextFieldWithBrowseButton()

    init {
        skaffoldBrowser.addBrowseFolderListener(
                CoreBundle.message("kubernetes.settings.dependencies.skaffold.browse.title"),
                null /*description*/,
                null /*project*/,
                FileChooserDescriptor(
                        true /*chooseFiles*/,
                        false /*chooseFolders*/,
                        false /*chooseJars*/,
                        false /*chooseJarsAsFiles*/,
                        false /*chooseJarContents*/,
                        false /*chooseMultiple*/))
    }

    override fun isModified(): Boolean {
        return KubernetesSettingsService.instance.skaffoldExecutablePath != skaffoldBrowser.text
    }

    override fun getDisplayName(): String = CoreBundle.message("kubernetes.settings.name")

    override fun apply() {
        KubernetesSettingsService.instance.skaffoldExecutablePath = skaffoldBrowser.text
    }

    override fun reset() {
        skaffoldBrowser.text = KubernetesSettingsService.instance.skaffoldExecutablePath
    }

    override fun createComponent(): JComponent? {
        skaffoldNotExecutableWarning.icon = AllIcons.General.Warning
        skaffoldNotExecutableWarning.foreground = Color.RED
        skaffoldNotExecutableWarning.isVisible = false

        val dependenciesPanel = panel {
            row(CoreBundle.message("kubernetes.settings.dependencies.skaffold.selector.title")) {
                skaffoldBrowser(grow)
            }

            noteRow(CoreBundle.message("kubernetes.settings.dependencies.skaffold.note"))

            row {
                skaffoldNotExecutableWarning()
            }
        }

        dependenciesPanel.border = IdeaTitledBorder(
                CoreBundle.message(
                        "kubernetes.settings.dependencies.panel.title"), 0, Insets(0, 0, 0, 0))

        // Wrapping in an extra panel so that the titled border shows up properly
        return panel {
            row {
                dependenciesPanel()
            }
        }
    }
}
