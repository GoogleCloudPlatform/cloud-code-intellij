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

import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.skaffold.message
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Base settings editor for both Skaffold single run and continunous run configurations. Includes
 * drop-down list of all Skaffold configuration files ([SkaffoldFilesComboBox]) found
 * in the project and basic validation of the currently selected Skaffold file.
 *
 * @param editorTitle title for the settings editor
 * @param helperText additional helper text for the settings editor
 * @param T type of the [AbstractSkaffoldRunConfiguration] this editor works with, i.e. run or dev.
 */
open class BaseSkaffoldSettingsEditor<T : AbstractSkaffoldRunConfiguration>(
    val editorTitle: String,
    val helperText: String = ""
) :
    SettingsEditor<T>() {

    @VisibleForTesting
    val skaffoldFilesComboBox = SkaffoldFilesComboBox()

    protected lateinit var basePanel: JPanel

    private val extensionComponents: MutableMap<String, JComponent> = mutableMapOf()

    /**
     * Registers additional custom components for Skaffold configuration UI.
     * @param newExtensionComponents extension components mapped to their label text
     */
    protected fun addExtensionComponents(newExtensionComponents: Map<String, JComponent>) {
        extensionComponents.putAll(newExtensionComponents)
    }

    override fun createEditor(): JComponent {
        basePanel = panel {
            row {
                label(helperText, 0, UIUtil.ComponentStyle.SMALL)
            }

            row(message("skaffold.configuration.label")) { skaffoldFilesComboBox(grow) }

            extensionComponents.forEach {
                row(it.key) { it.value(grow) }
            }
        }

        basePanel.border = IdeaTitledBorder(editorTitle, 0, Insets(0, 0, 0, 0))

        return basePanel
    }

    override fun applyEditorTo(runConfig: T) {
        val selectedSkaffoldFile: VirtualFile =
            skaffoldFilesComboBox.getItemAt(skaffoldFilesComboBox.selectedIndex)
                ?: throw ConfigurationException(message("skaffold.no.file.selected.error"))

        if (!SkaffoldFileService.instance.isSkaffoldFile(selectedSkaffoldFile)) {
            throw ConfigurationException(message("skaffold.invalid.file.error"))
        }

        // save properties
        runConfig.skaffoldConfigurationFilePath = selectedSkaffoldFile.path
    }

    override fun resetEditorFrom(runConfig: T) {
        skaffoldFilesComboBox.setProject(runConfig.project)
        runConfig.skaffoldConfigurationFilePath?.let {
            LocalFileSystem.getInstance().findFileByPath(it)
        }?.let { skaffoldFilesComboBox.setSelectedSkaffoldFile(it) }
    }
}
