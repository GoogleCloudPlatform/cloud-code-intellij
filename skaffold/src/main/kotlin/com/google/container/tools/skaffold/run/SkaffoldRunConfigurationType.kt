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

import com.google.container.tools.skaffold.SKAFFOLD_ICON
import com.google.container.tools.skaffold.message
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

/**
 * [ConfigurationType] extension that registers and enables Skaffold run configuration type,
 * including single run and continuous deployment targets provided by separate
 * [ConfigurationFactory].
 */
class SkaffoldRunConfigurationType : ConfigurationType {
    val ID = "google-container-tools-skaffold-run-config"

    val skaffoldSingleRunConfigurationFactory = SkaffoldSingleRunConfigurationFactory(this)
    val skaffoldDevConfigurationFactory = SkaffoldDevConfigurationFactory(this)

    override fun getIcon(): Icon = SKAFFOLD_ICON

    override fun getConfigurationTypeDescription(): String =
        message("skaffold.run.config.general.description")

    override fun getId(): String = ID

    override fun getDisplayName(): String = message("skaffold.run.config.general.name")

    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(skaffoldSingleRunConfigurationFactory, skaffoldDevConfigurationFactory)
}

/**
 * Factory for Skaffold single run ("skaffold run" style) configurations. See template configuration
 * at [SkaffoldSingleRunConfiguration].
 */
class SkaffoldSingleRunConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    val RUN_ID = "google-container-tools-skaffold-run-config-run"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        SkaffoldSingleRunConfiguration(project, this, name)

    override fun getName(): String = message("skaffold.run.config.single.run.name")

    override fun getId(): String = RUN_ID
}

/**
 * Factory for Skaffold dev (continuous) mode ("skaffold dev" style) configurations.
 * See template configuration at [SkaffoldDevConfiguration].
 */
class SkaffoldDevConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    val DEV_ID = "google-container-tools-skaffold-run-config-dev"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        SkaffoldDevConfiguration(project, this, name)

    override fun getName(): String = message("skaffold.run.config.dev.run.name")

    override fun getIcon(): Icon = SKAFFOLD_ICON

    override fun getId(): String = DEV_ID
}
