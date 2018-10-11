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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute

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

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        null

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
        null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        SkaffoldDevSettingsEditor()
}

/**
 * Base class for Skaffold run configurations, includes base properties such as Skaffold
 * configuration file path.
 */
abstract class AbstractSkaffoldRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase(project, factory, name) {

    /**
     * Persisted Skaffold config file absolute path for Skaffold run configurations.
     * See more at [com.intellij.openapi.vfs.VirtualFile.getPath]
     */
    @Attribute("skaffoldConfigurationFilePath")
    var skaffoldConfigurationFilePath: String? = null
}
