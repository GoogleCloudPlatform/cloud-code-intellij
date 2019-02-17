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
import com.google.container.tools.core.analytics.UsageTrackerProvider
import com.google.container.tools.skaffold.metrics.SKAFFOLD_AUTO_CREATE_CONFIGURATIONS
import com.google.container.tools.skaffold.run.AbstractSkaffoldRunConfiguration
import com.google.container.tools.skaffold.run.SkaffoldDevConfigurationFactory
import com.google.container.tools.skaffold.run.SkaffoldRunConfigurationType
import com.google.container.tools.skaffold.run.SkaffoldSingleRunConfigurationFactory
import com.intellij.ProjectTopics
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.yaml.YAMLFileType

/**
 * Detects if project has Skaffold configuration but no Skaffold run targets configured. Uses
 * [ProjectComponent] to watch project opening, and prompts to create dev/deploy configurations
 * if they don't exist yet.
 *
 * Uses [VirtualFileManager] and [VirtualFileListener] to track new YAML files in already opened
 * project to prompt creating new configurations when Skaffold YAML is created.
 *
 * @param project IDE Project for which this component is created.
 */
class SkaffoldConfigurationDetector(val project: Project) : ProjectComponent {

    private val logger = Logger.getInstance(SkaffoldConfigurationDetector::class.java)

    private val NOTIFICATION_GROUP = NotificationGroup(
        PLUGIN_NOTIFICATION_DISPLAY_GROUP_ID,
        NotificationDisplayType.BALLOON,
        true,
        null,
        SKAFFOLD_ICON
    )

    override fun projectOpened() {
        // on project open there may be a lot of indexing and other preparation and files still
        // not available, wait for "smart" mode for configuration identification
        DumbService.getInstance(project).runWhenSmart {
            checkSkaffoldConfigurationsNeeded()
        }

        // when a project is initialized from external build system for the first time (i.e. Maven),
        // modules are not available immediately. Use message bus to check for modules once they are
        // actually available
        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(project: Project, module: Module) {
                DumbService.getInstance(project).runWhenSmart {
                    checkSkaffoldConfigurationsNeeded()
                }
            }
        })

        // add VFS listener to ensure we track YAML file changes when no Skaffold configurations
        // exist yet to show prompt for configurations once Skaffold file is created.
        addVirtualFileListener(object : VirtualFileListener {
            override fun contentsChanged(event: VirtualFileEvent) {
                checkForSkaffoldFile(event.file)
            }

            override fun fileCreated(event: VirtualFileEvent) {
                checkForSkaffoldFile(event.file)
            }

            private fun checkForSkaffoldFile(file: VirtualFile) {
                DumbService.getInstance(project).runWhenSmart {
                    if (file.fileType is YAMLFileType &&
                        SkaffoldFileService.instance.isSkaffoldFile(
                            file
                        ) && !hasExistingSkaffoldConfigurations()
                    ) {
                        // content changed to be a valid Skaffold file,
                        // and no Skaffold configurations exist, prompt
                        showPromptForSkaffoldConfigurations(project, file)
                    }
                }
            }
        })
    }

    /**
     * Lists existing skaffold configuration files and in case no run configuration targets for
     * Skaffold exist, prompts to create them with pop-up notification.
     */
    private fun checkSkaffoldConfigurationsNeeded() {
        val skaffoldFiles: List<VirtualFile> =
            SkaffoldFileService.instance.findSkaffoldFiles(project)
        if (skaffoldFiles.isNotEmpty()) {
            if (!hasExistingSkaffoldConfigurations()) {
                // existing Skaffold config files, but no skaffold configurations, prompt
                showPromptForSkaffoldConfigurations(project, skaffoldFiles[0])
            }
        }
    }

    /**
     * Checks if the project has existing Skaffold run configurations. Project is current, one
     * per created component, passed a parameter.
     */
    private fun hasExistingSkaffoldConfigurations() =
        getRunManager(project)
            .allConfigurationsList.filter { it is AbstractSkaffoldRunConfiguration }.isNotEmpty()

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

                // single client tracking ping since we offer single menu item
                // to create both deploy/dev configurations at once
                UsageTrackerProvider.instance.usageTracker.trackEvent(
                    SKAFFOLD_AUTO_CREATE_CONFIGURATIONS
                ).ping()

                notification.expire()
            }
        })

        notification.notify(project)
    }

    @VisibleForTesting
    fun addSkaffoldDevConfiguration(skaffoldFilePath: String) {
        createRunConfigurationFromSettings(
            SkaffoldDevConfigurationFactory.DEV_ID,
            message("skaffold.run.config.dev.default.name"),
            skaffoldFilePath
        )
    }

    @VisibleForTesting
    fun addSkaffoldRunConfiguration(skaffoldFilePath: String) {
        createRunConfigurationFromSettings(
            SkaffoldSingleRunConfigurationFactory.RUN_ID,
            message("skaffold.run.config.run.default.name"),
            skaffoldFilePath
        )
    }

    /** Creates a run configuration from the given settings and selects it in run combobox */
    private fun createRunConfigurationFromSettings(
        factoryId: String,
        name: String,
        skaffoldFilePath: String
    ) {

        val factory = findConfigurationFactoryById(factoryId)
        if (factory == null) {
            logger.error("Skaffold configuration factory not found, plugin install is corrupted.")
            return
        }

        val runnerAndConfigSettings = getRunManager(project).createConfiguration(name, factory)
        (runnerAndConfigSettings.configuration as AbstractSkaffoldRunConfiguration)
            .skaffoldConfigurationFilePath = skaffoldFilePath
        getRunManager(project).addConfiguration(runnerAndConfigSettings)
        getRunManager(project).selectedConfiguration = runnerAndConfigSettings
    }

    @VisibleForTesting
    fun findConfigurationFactoryById(factoryId: String): ConfigurationFactory? {
        val configurationType =
            ConfigurationTypeUtil.findConfigurationType(SkaffoldRunConfigurationType.ID)
        return configurationType?.let {
            it.configurationFactories.filter { it.id == factoryId }[0]
        }
    }

    @VisibleForTesting
    fun createNotification(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFORMATION
    ): Notification =
        NOTIFICATION_GROUP.createNotification(
            title, null /* subtitle */, message, type
        )

    @VisibleForTesting
    fun getRunManager(project: Project): RunManager =
        RunManager.getInstance(project)

    /**
     * Adds [VirtualFileListener] to [VirtualFileManager] to track project's file system changes
     * and detect new file and content changes.
     */
    @VisibleForTesting
    fun addVirtualFileListener(listener: VirtualFileListener) {
        VirtualFileManager.getInstance().addVirtualFileListener(
            listener,
            project /* project used as a disposable to remove listener when project closes */
        )
    }
}
