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

import com.google.container.tools.skaffold.findSkaffoldFiles
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.MutableComboBoxModel

/**
 * A combo box with a list of available Skaffold configuration files for a given project.
 */
class SkaffoldFilesComboBox : JComboBox<VirtualFile>() {
    private lateinit var skaffoldFilesMutableModel: MutableComboBoxModel<VirtualFile>

    /**
     * Sets project for the combo box, and fills combo box with all Skaffold files available
     * in this project. First file is selected by default.
     */
    fun setProject(project: Project) {
        setRenderer(VirtualFileRenderer(project.baseDir))
        skaffoldFilesMutableModel =
            DefaultComboBoxModel<VirtualFile>(findSkaffoldFiles(project).toTypedArray())
        model = skaffoldFilesMutableModel
        if (model.size > 0) selectedIndex = 0
    }

    /**
     * Sets given file as a selected Skaffold file in the drop-down. If model does not contain
     * the file, adds it and selects it.
     */
    fun setSelectedSkaffoldFile(skaffoldFile: VirtualFile) {
        skaffoldFilesMutableModel.addElement(skaffoldFile)
        selectedItem = skaffoldFile
    }
}

/** Renders name of the Skaffold file relative to the base dir of the current IDE project. */
private class VirtualFileRenderer(val projectBaseDir: VirtualFile) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val renderer =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (value is VirtualFile && renderer is JLabel) {
            renderer.text = VfsUtilCore.getRelativeLocation(value, projectBaseDir)
        }

        return renderer
    }
}
