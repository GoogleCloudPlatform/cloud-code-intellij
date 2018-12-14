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

import com.google.container.tools.skaffold.SkaffoldExecutorService
import com.google.container.tools.skaffold.SkaffoldExecutorSettings
import com.google.container.tools.skaffold.SkaffoldLabels
import com.google.container.tools.skaffold.message
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Runs Skaffold on command line in both "run"/"dev" modes based on the given
 * [AbstractSkaffoldRunConfiguration] and current IDE project. [startProcess] checks configuration
 * and constructs command line to launch Skaffold process. Base class manages console
 * window output and graceful process shutdown (also see [KillableProcessHandler])
 *
 * @param environment Execution environment provided by IDE
 * @param executionMode Skaffold  mode execution, i.e. DEV
 */
class SkaffoldCommandLineState(
    environment: ExecutionEnvironment,
    val executionMode: SkaffoldExecutorSettings.ExecutionMode
) : CommandLineState(environment) {
    public override fun startProcess(): ProcessHandler {
        val runConfiguration: RunConfiguration? =
            environment.runnerAndConfigurationSettings?.configuration
        val projectBaseDir: VirtualFile? = environment.project.guessProjectDir()
        // ensure the configuration is valid for execution - settings are of supported type,
        // project is valid and Skaffold file is present.
        if (runConfiguration == null || runConfiguration !is AbstractSkaffoldRunConfiguration ||
            projectBaseDir == null
        ) {
            throw ExecutionException(message("skaffold.corrupted.run.settings"))
        }
        if (runConfiguration.skaffoldConfigurationFilePath == null) {
            throw ExecutionException(message("skaffold.no.file.selected.error"))
        }

        val configFile: VirtualFile? = LocalFileSystem.getInstance()
            .findFileByPath(runConfiguration.skaffoldConfigurationFilePath!!)
        // use project dir relative location for cleaner command line representation
        val skaffoldConfigurationFilePath: String? = VfsUtilCore.getRelativeLocation(
            configFile, projectBaseDir
        )

        // custom settings for single deployment (run) mode
        val singleRunConfiguration: SkaffoldSingleRunConfiguration? =
            if (runConfiguration is SkaffoldSingleRunConfiguration) runConfiguration else null

        val skaffoldProcess = SkaffoldExecutorService.instance.executeSkaffold(
            SkaffoldExecutorSettings(
                executionMode,
                skaffoldConfigurationFilePath,
                skaffoldProfile = runConfiguration.skaffoldProfile,
                workingDirectory = File(projectBaseDir.path),
                skaffoldLabels = SkaffoldLabels.defaultLabels,
                tailLogsAfterDeploy = singleRunConfiguration?.tailDeploymentLogs
            )
        )

        return KillableProcessHandler(skaffoldProcess.process, skaffoldProcess.commandLine)
    }
}
