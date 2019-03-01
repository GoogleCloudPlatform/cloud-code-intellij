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

package com.google.kubernetes.tools.skaffold

import com.google.cloud.tools.intellij.analytics.UsageTrackerService
import com.google.common.annotations.VisibleForTesting
import com.google.container.tools.core.settings.KubernetesSettingsService
import com.google.kubernetes.tools.skaffold.SkaffoldExecutorSettings.ExecutionMode
import com.google.kubernetes.tools.skaffold.metrics.METADATA_ERROR_MESSAGE_KEY
import com.google.kubernetes.tools.skaffold.metrics.SKAFFOLD_DEV_RUN_FAIL
import com.google.kubernetes.tools.skaffold.metrics.SKAFFOLD_DEV_RUN_SUCCESS
import com.google.kubernetes.tools.skaffold.metrics.SKAFFOLD_SINGLE_RUN_FAIL
import com.google.kubernetes.tools.skaffold.metrics.SKAFFOLD_SINGLE_RUN_SUCCESS
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
    protected abstract fun getSkaffoldExecutablePath(): Path

    fun getSystemPath(): String = System.getenv("PATH")

    fun isSkaffoldAvailable(): Boolean {
        val skaffoldPathOverride = KubernetesSettingsService.instance.skaffoldExecutablePath

        val isSkaffoldOnPath = getSystemPath().split(File.pathSeparator)
                .asSequence()
                .map { it + File.separator + "skaffold" }
                .any {
                    File(it).exists() && File(it).canExecute()
                }

        return skaffoldPathOverride.isNotBlank() || isSkaffoldOnPath
    }

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
            add(getSkaffoldExecutablePath().toString())
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

            settings.defaultImageRepo?.let {
                add("--default-repo")
                add(it)
            }
        }

        try {
            val skaffoldProcess = SkaffoldProcess(
                createProcess(settings.workingDirectory, commandList),
                commandLine = commandList.joinToString(" ")
            )

            // track event based on execution mode.
            val skaffoldEventName =
                if (settings.executionMode == ExecutionMode.DEV)
                    SKAFFOLD_DEV_RUN_SUCCESS
                else
                    SKAFFOLD_SINGLE_RUN_SUCCESS
            UsageTrackerService.getInstance().trackEvent(skaffoldEventName).ping()

            return skaffoldProcess
        } catch (e: Exception) {
            val skaffoldFailEventName =
                if (settings.executionMode == ExecutionMode.DEV)
                    SKAFFOLD_DEV_RUN_FAIL
                else
                    SKAFFOLD_SINGLE_RUN_FAIL

            UsageTrackerService.getInstance().trackEvent(skaffoldFailEventName)
                .addMetadata(
                    METADATA_ERROR_MESSAGE_KEY, e.javaClass.name
                ).ping()
            // re-throw exception to make sure a user sees run resulted in an error and sees error
            // message.
            throw e
        }
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
 * @property defaultImageRepo Default image repository to use instead of repo defined in Skaffold
 *           and Kubernetes manifests.
 */
data class SkaffoldExecutorSettings(
    val executionMode: ExecutionMode,
    val skaffoldConfigurationFilePath: String? = null,
    val skaffoldProfile: String? = null,
    val workingDirectory: File? = null,
    val skaffoldLabels: SkaffoldLabels? = null,
    val tailLogsAfterDeploy: Boolean? = null,
    val defaultImageRepo: String? = null
) {

    /** Execution mode for Skaffold, single run, continuous development (run or debug mode), etc. */
    enum class ExecutionMode(val modeFlag: String) {
        SINGLE_RUN("run"),
        DEV("dev"),
        DEBUG("debug")
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
 * Default implementation of Skaffold executor service. The Skaffold executable is first pulled
 * from the Kubernetes settings, and then the PATH if unavailable in the settings.
 */
class DefaultSkaffoldExecutorService : SkaffoldExecutorService() {
    /**
     * First try to find Skaffold in the Kubernetes settings under Google -> Kubernetes.
     * If not found, then try to find it on the user's PATH.
     */
    override fun getSkaffoldExecutablePath(): Path {
        val skaffoldPathOverride = KubernetesSettingsService.instance.skaffoldExecutablePath

        return if (skaffoldPathOverride.isNotBlank()) {
            Paths.get(skaffoldPathOverride.trim())
        } else {
            Paths.get("skaffold")
        }
    }
}
