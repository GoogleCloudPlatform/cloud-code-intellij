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

package com.google.container.tools.core

import com.google.container.tools.core.util.CoreBundle.message
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import java.io.File
import javax.swing.JComponent

/**
 * Runs Skaffold on command line in both "run"/"dev" modes based on the given
 * [AbstractSkaffoldRunConfiguration] and current IDE project. [startProcess] checks configuration
 * and constructs command line to launch Skaffold process. Base class manages console
 * window output and graceful process shutdown (also see [KillableProcessHandler])
 *
 * @param environment Execution environment provided by IDE
 * @param executionMode Skaffold  mode execution, i.e. DEV
 */
class KubectlCommandLineState(
    environment: ExecutionEnvironment,
    val executionMode: KubectlExecutorSettings.ExecutionMode
) : CommandLineState(environment) {

    public override fun startProcess(): ProcessHandler {

        val runConfiguration: RunConfiguration? =
            environment.runnerAndConfigurationSettings?.configuration
        val projectBaseDir: VirtualFile? = environment.project.guessProjectDir()
        // ensure the configuration is valid for execution - settings are of supported type,
        // project is valid and Skaffold file is present.
        if (runConfiguration == null  || runConfiguration !is KubectlRunConfiguration || projectBaseDir == null) {
            throw ExecutionException(message("kubectl.corrupted.run.settings"))
        }
        if (runConfiguration.kubectlConfigurationFilePath == null) {
            throw ExecutionException(message("kubectl.no.file.selected.error"))
        }

        if (!KubectlExecutorService.instance.isKubectlAvailable()) {
            throw ExecutionException(message("kubectl.not.on.system.error"))
        }

        val configFile: VirtualFile? = LocalFileSystem.getInstance()
            .findFileByPath(runConfiguration.kubectlConfigurationFilePath!!)
        // use project dir relative location for cleaner command line representation
        val skaffoldConfigurationFilePath: String? = VfsUtilCore.getRelativeLocation(
            configFile, projectBaseDir
        )
//kubectlConfigurationFilePath,
//                skaffoldProfile = runConfiguration.skaffoldProfile,
// skaffoldLabels = SkaffoldLabels.defaultLabels,
//                tailLogsAfterDeploy = singleRunConfiguration?.tailDeploymentLogs,
//                defaultImageRepo = runConfiguration.imageRepositoryOverride

        val kubectlProcess = KubectlExecutorService.instance.executeKubectl(
            KubectlExecutorSettings(
                executionMode = executionMode,
                workingDirectory = File(projectBaseDir.path),
                executionFlags = listOf()
            )
        )
        return KillableProcessHandler(kubectlProcess.process, kubectlProcess.commandLine)
    }
}

class KubectlRunConfiguration( project: Project,
        factory: ConfigurationFactory,
        name: String
) : RunConfigurationBase<Element>(project, factory, name) {

    /**
     * Persisted Skaffold config file absolute path for Skaffold run configurations.
     * See more at [com.intellij.openapi.vfs.VirtualFile.getPath]
     */
    var kubectlConfigurationFilePath: String? = null

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

    override fun checkConfiguration() {
        if (!KubectlExecutorService.instance.isKubectlAvailable()) {
            throw RuntimeConfigurationWarning(message("kubectl.not.on.system.error"))
        }
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = object : SettingsEditor<RunConfiguration>(){
        override fun resetEditorFrom(s: RunConfiguration) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun createEditor(): JComponent {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun applyEditorTo(s: RunConfiguration) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


}