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
import com.google.container.tools.skaffold.message
import javax.swing.JCheckBox

/**
 * Settings editor that provides a UI for Skaffold single mode run configuration settings,
 * also saves and retrieves the settings from the project state.
 */
class SkaffoldSingleRunSettingsEditor :
    BaseSkaffoldSettingsEditor<SkaffoldSingleRunConfiguration>(
        editorTitle = message("skaffold.run.config.single.run.name"),
        helperText = message("skaffold.run.config.single.run.helperText")
    ) {

    @VisibleForTesting
    val tailLogsCheckbox = JCheckBox()

    init {
        addExtensionComponents(mapOf(message("skaffold.tail.logs.label") to tailLogsCheckbox))
    }

    override fun applyEditorTo(runConfig: SkaffoldSingleRunConfiguration) {
        super.applyEditorTo(runConfig)

        runConfig.tailDeploymentLogs = tailLogsCheckbox.isSelected
    }

    override fun resetEditorFrom(runConfig: SkaffoldSingleRunConfiguration) {
        super.resetEditorFrom(runConfig)

        tailLogsCheckbox.isSelected = runConfig.tailDeploymentLogs
    }
}
