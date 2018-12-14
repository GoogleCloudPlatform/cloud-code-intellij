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
import com.google.container.tools.skaffold.SkaffoldExecutorSettings.ExecutionMode
import com.intellij.openapi.components.ServiceManager
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Abstract implementation for Skaffold execution service. This service builds and launches Skaffold
 * process from the given set of standard flags and settings.
 * Constructs Skaffold command line and build resulting process.
 * Path to Skaffold executable must be set by subclasses.
 * [DefaultSkaffoldExecutorService] assumes Skaffold to be already installed and available.
 */
abstract class SkaffoldExecutorService {
    companion object {
        val instance
            get() = ServiceManager.getService(SkaffoldExecutorService::class.java)!!
    }

    /** Path for Skaffold executable, any form supported by [ProcessBuilder] */
    protected abstract var skaffoldExecutablePath: Path

    /**
     * Creates Skaffold command line from the given settings and returns resulting launched
     * Skaffold system process.
     *
     * @param settings Settings with flags supported by Skaffold.
     * @return [SkaffoldProcess] with resulting process and the command line used to launch it.
     */
    fun executeSkaffold(settings: SkaffoldExecutorSettings):
        SkaffoldProcess {
        val commandList = mutableListOf<String>()
        with(commandList) {
            add(skaffoldExecutablePath.toString())
            add(settings.executionMode.modeFlag)
            settings.skaffoldConfigurationFilePath?.let {
                add("--filename")
                add(it)
            }
            settings.skaffoldProfile?.let {
                add("--profile")
                add(it)
            }
            settings.skaffoldLabels?.let {
                it.labels
                    .forEach { label ->
                        add("--label")
                        add("${label.key}=${label.value}")
                    }
            }

            settings.tailLogsAfterDeploy?.let { tailLogs ->
                if (tailLogs) add("--tail")
            }
        }

        return SkaffoldProcess(
            createProcess(settings.workingDirectory, commandList),
            commandLine = commandList.joinToString(" ")
        )
    }

    @VisibleForTesting
    fun createProcess(
        workingDirectory: File?,
        commandList: List<String>
    ): Process {
        val processBuilder = ProcessBuilder()
        workingDirectory?.let { processBuilder.directory(it) }

        return processBuilder.command(commandList).start()
    }
}

/**
 * Set of settings to control Skaffold execution, including flags and execution mode.
 *
 * @property executionMode Mandatory execution mode for Skaffold, see [ExecutionMode].
 * @property skaffoldConfigurationFilePath Optional, location of the Skaffold YAML
 *           configuration file. If not provided, default `skaffold.yaml` used.
 * @property skaffoldProfile Skaffold profile name, optional.
 * @property workingDirectory Optional, working directory where Skaffold needs to be launched.
 *           This is usually set to project working directory.
 * @property skaffoldLabels Kubernetes style labels to pass to Skaffold execution.
 */
data class SkaffoldExecutorSettings(
    val executionMode: ExecutionMode,
    val skaffoldConfigurationFilePath: String? = null,
    val skaffoldProfile: String? = null,
    val workingDirectory: File? = null,
    val skaffoldLabels: SkaffoldLabels? = null,
    val tailLogsAfterDeploy: Boolean? = null
) {

    /** Execution mode for Skaffold, single run, continuous development, etc. */
    enum class ExecutionMode(val modeFlag: String) {
        SINGLE_RUN("run"),
        DEV("dev")
    }
}

/**
 * Data object with launched Skaffold process and its command line.
 *
 * @property process System process for Skaffold.
 * @property commandLine Command line used to launch the process.
 */
data class SkaffoldProcess(val process: Process, val commandLine: String)

/**
 * Default implementation of Skaffold execution service, where Skaffold executable is assumed to
 * be already installed on the system and be available in PATH.
 */
class DefaultSkaffoldExecutorService : SkaffoldExecutorService() {
    // use executable available in PATH
    override var skaffoldExecutablePath: Path = Paths.get("skaffold")
}
