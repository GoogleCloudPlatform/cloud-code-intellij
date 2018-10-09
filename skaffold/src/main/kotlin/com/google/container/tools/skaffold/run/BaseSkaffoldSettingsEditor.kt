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

import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.skaffold.message
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.panel
import javax.swing.JComponent

/**
 * Base settings editor for both Skaffold single run and continunous run configurations. Includes
 * drop-down list of all Skaffold configuration files ([SkaffoldFilesComboBox]) found
 * in the project and basic validation of the currently selected Skaffold file.
 */
open class BaseSkaffoldSettingsEditor : SettingsEditor<RunConfiguration>() {
    private lateinit var skaffoldFilesComboBox: SkaffoldFilesComboBox

    override fun createEditor(): JComponent {
        skaffoldFilesComboBox = SkaffoldFilesComboBox()
        val basePanel = panel {
            row(message("skaffold.configuration.label")) { skaffoldFilesComboBox(grow) }
        }

        return basePanel
    }

    override fun applyEditorTo(runConfig: RunConfiguration) {
        val selectedSkaffoldFile: VirtualFile =
            skaffoldFilesComboBox.getItemAt(skaffoldFilesComboBox.selectedIndex)
                ?: throw ConfigurationException(message("skaffold.no.file.selected.error"))

        if (!SkaffoldFileService.instance.isSkaffoldFile(selectedSkaffoldFile)) {
            throw ConfigurationException(message("skaffold.invalid.file.error"))
        }
    }

    override fun resetEditorFrom(runConfig: RunConfiguration) {
        skaffoldFilesComboBox.setProject(runConfig.project)
    }
}
