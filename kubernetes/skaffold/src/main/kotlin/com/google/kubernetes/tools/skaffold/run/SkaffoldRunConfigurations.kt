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

package com.google.kubernetes.tools.skaffold.run

import com.google.common.annotations.VisibleForTesting
import com.google.kubernetes.tools.skaffold.SkaffoldExecutorSettings
import com.google.kubernetes.tools.skaffold.run.ui.SkaffoldDevSettingsEditor
import com.google.kubernetes.tools.skaffold.run.ui.SkaffoldSingleRunSettingsEditor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element

/**
 * Template configuration for Skaffold single run configuration, serving as a base for all new
 * configurations created by a user. Has its own UI settings and editor and provides execution state
 * once "skaffold run" executes.
 */
class SkaffoldSingleRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : AbstractSkaffoldRunConfiguration(project, factory, name) {

    /** Single run tail logs option to stream logs after deployment until stopped. */
    var tailDeploymentLogs: Boolean = false

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        SkaffoldCommandLineState(environment, SkaffoldExecutorSettings.ExecutionMode.SINGLE_RUN)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        SkaffoldSingleRunSettingsEditor()
}

/**
 * Template configuration for Skaffold dev mode run configuration, serving as a base for all new
 * configurations created by a user. Has its own UI settings and editor and provides execution state
 * once "skaffold dev" command executes.
 */
class SkaffoldDevConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : AbstractSkaffoldRunConfiguration(project, factory, name) {

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        SkaffoldCommandLineState(environment, getExecutionMode(environment))

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        SkaffoldDevSettingsEditor()

    /**
     * Returns the execution mode depending on the execution environment, i.e. if it was run in
     * either 'run' or 'debug' mode.
     */
    @VisibleForTesting
    fun getExecutionMode(environment: ExecutionEnvironment) = when (environment.executor) {
            is DefaultDebugExecutor -> SkaffoldExecutorSettings.ExecutionMode.DEBUG
            else -> SkaffoldExecutorSettings.ExecutionMode.DEV
        }
}

/**
 * Base class for Skaffold run configurations, includes base properties such as Skaffold
 * configuration file path.
 */
abstract class AbstractSkaffoldRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<Element>(project, factory, name) {

    /**
     * Persisted Skaffold config file absolute path for Skaffold run configurations.
     * See more at [com.intellij.openapi.vfs.VirtualFile.getPath]
     */
    var skaffoldConfigurationFilePath: String? = null

    var skaffoldProfile: String? = null

    /** Image repository to use with a Skaffold run target instead of one configured by default. */
    var imageRepositoryOverride: String? = null

    override fun readExternal(element: Element) {
        super.readExternal(element)

        XmlSerializer.deserializeInto(this, element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        XmlSerializer.serializeInto(this, element)
    }
}
