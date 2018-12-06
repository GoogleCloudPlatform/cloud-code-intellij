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

import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.core.PLUGIN_NOTIFICATION_DISPLAY_GROUP_ID
import com.google.container.tools.skaffold.run.AbstractSkaffoldRunConfiguration
import com.google.container.tools.skaffold.run.SkaffoldDevConfiguration
import com.google.container.tools.skaffold.run.SkaffoldDevConfigurationFactory
import com.google.container.tools.skaffold.run.SkaffoldRunConfigurationType
import com.google.container.tools.skaffold.run.SkaffoldSingleRunConfiguration
import com.google.container.tools.skaffold.run.SkaffoldSingleRunConfigurationFactory
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Detects if project has Skaffold configuration but no Skaffold run targets configured. Uses
 * [ProjectComponent] to watch project opening, and prompts to create dev/deploy configurations
 * if they don't exist yet.
 *
 * @param project IDE Project for which this component is created.
 */
class SkaffoldConfigurationDetector(val project: Project) : ProjectComponent {
    private val NOTIFICATION_GROUP = NotificationGroup(
        PLUGIN_NOTIFICATION_DISPLAY_GROUP_ID,
        NotificationDisplayType.BALLOON,
        true,
        null,
        SKAFFOLD_ICON
    )

    override fun projectOpened() {
        val skaffoldFiles: List<VirtualFile> =
            SkaffoldFileService.instance.findSkaffoldFiles(project)
        if (skaffoldFiles.isNotEmpty()) {
            val skaffoldRunConfigList: List<RunConfiguration> =
                getRunManager(project)
                    .allConfigurationsList.filter { it is AbstractSkaffoldRunConfiguration }
            if (skaffoldRunConfigList.isEmpty()) {
                // existing Skaffold config files, but no skaffold configurations, prompt
                showPromptForSkaffoldConfigurations(project, skaffoldFiles[0])
            }
        }
    }

    /**
     * Prepares an IDE notification if no Skaffold configurations exist, adding actions to add
     * both dev/deploy configuration, or only one of them.
     */
    private fun showPromptForSkaffoldConfigurations(
        project: Project,
        skaffoldFile: VirtualFile
    ) {
        val notification: Notification = createNotification(
            message("skaffold.detect.notification.title"),
            message("skaffold.detect.notification.message")
        )

        notification.addAction(object :
            AnAction(message("skaffold.detect.notification.add.both.configs")) {
            override fun actionPerformed(e: AnActionEvent) {
                addSkaffoldRunConfiguration(skaffoldFile.path)
                addSkaffoldDevConfiguration(skaffoldFile.path)
                notification.expire()
            }
        })

        notification.notify(project)
    }

    @VisibleForTesting
    fun addSkaffoldDevConfiguration(skaffoldFilePath: String) {
        val skaffoldDevSettings = SkaffoldDevConfiguration(
            project,
            SkaffoldDevConfigurationFactory(SkaffoldRunConfigurationType()),
            message("skaffold.run.config.dev.default.name")
        )
        skaffoldDevSettings.skaffoldConfigurationFilePath = skaffoldFilePath

        createRunConfigurationFromSettings(skaffoldDevSettings)
    }

    @VisibleForTesting
    fun addSkaffoldRunConfiguration(skaffoldFilePath: String) {
        val skaffoldRunSettings = SkaffoldSingleRunConfiguration(
            project,
            SkaffoldSingleRunConfigurationFactory(SkaffoldRunConfigurationType()),
            message("skaffold.run.config.run.default.name")
        )
        skaffoldRunSettings.skaffoldConfigurationFilePath = skaffoldFilePath

        createRunConfigurationFromSettings(skaffoldRunSettings)
    }

    /** Creates a run configuration from the given settings and selects it in run combobox */
    private fun createRunConfigurationFromSettings(runConfiguration: RunConfiguration) {
        val runnerAndConfigSettings = RunnerAndConfigurationSettingsImpl(
            RunManagerImpl.getInstanceImpl(project),
            runConfiguration
        )
        getRunManager(project).addConfiguration(runnerAndConfigSettings)
        getRunManager(project).selectedConfiguration = runnerAndConfigSettings
    }

    @VisibleForTesting
    fun createNotification(title: String, message: String): Notification =
        NOTIFICATION_GROUP.createNotification(
            title, null /* subtitle */, message, NotificationType.INFORMATION
        )

    @VisibleForTesting
    fun getRunManager(project: Project): RunManager =
        RunManager.getInstance(project)
}
