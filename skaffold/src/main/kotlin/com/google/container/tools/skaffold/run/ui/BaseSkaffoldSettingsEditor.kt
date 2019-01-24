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

import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.skaffold.message
import com.google.container.tools.skaffold.run.AbstractSkaffoldRunConfiguration
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.TitledSeparator
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.components.JBTextField
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

    @VisibleForTesting
    val skaffoldProfilesComboBox = SkaffoldProfilesComboBox()

    @VisibleForTesting
    val overrideImageRepoTextField = JBTextField()

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

            row(message("skaffold.profile.label")) { skaffoldProfilesComboBox(grow) }

            extensionComponents.forEach {
                row(it.key) { it.value(grow) }
            }

            row {
                TitledSeparator(message("skaffold.image.options.subtitle"))(grow)
            }
            row(message("skaffold.override.image.repo")) { overrideImageRepoTextField(grow) }
        }

        basePanel.border = IdeaTitledBorder(editorTitle, 0, Insets(0, 0, 0, 0))

        overrideImageRepoTextField.emptyText.text =
            message("skaffold.override.image.repo.empty.prompt")
        overrideImageRepoTextField.toolTipText = message("skaffold.override.image.repo.tooltip")

        skaffoldFilesComboBox.addActionListener {
            skaffoldProfilesComboBox.skaffoldFileUpdated(
                skaffoldFilesComboBox.getSelectedSkaffoldFile()
            )
        }

        return basePanel
    }

    override fun applyEditorTo(runConfig: T) {
        val selectedSkaffoldFile: VirtualFile =
            skaffoldFilesComboBox.getSelectedSkaffoldFile()
                ?: throw ConfigurationException(message("skaffold.no.file.selected.error"))

        if (!SkaffoldFileService.instance.isSkaffoldFile(selectedSkaffoldFile)) {
            throw ConfigurationException(message("skaffold.invalid.file.error"))
        }

        // save properties
        runConfig.skaffoldConfigurationFilePath = selectedSkaffoldFile.path
        runConfig.skaffoldProfile = skaffoldProfilesComboBox.getSelectedProfile()
        // do not save empty repository name, convert to null
        runConfig.imageRepositoryOverride =
            overrideImageRepoTextField.text.let { if (it.isEmpty()) null else it }
    }

    override fun resetEditorFrom(runConfig: T) {
        skaffoldFilesComboBox.setProject(runConfig.project)
        skaffoldProfilesComboBox.project = runConfig.project

        runConfig.skaffoldConfigurationFilePath?.let {
            LocalFileSystem.getInstance().findFileByPath(it)
        }?.let { skaffoldFilesComboBox.setSelectedSkaffoldFile(it) }

        runConfig.skaffoldProfile?.let {
            skaffoldProfilesComboBox.setSelectedProfile(it)
        }

        runConfig.imageRepositoryOverride?.let {
            overrideImageRepoTextField.text = it
        }
    }
}
