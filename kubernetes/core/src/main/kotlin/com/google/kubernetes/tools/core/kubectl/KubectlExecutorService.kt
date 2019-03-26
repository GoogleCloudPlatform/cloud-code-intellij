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

package com.google.kubernetes.tools.core.kubectl

import com.intellij.execution.ExecutionException
import com.google.cloud.tools.intellij.analytics.UsageTrackerService
import com.google.common.annotations.VisibleForTesting
import com.google.kubernetes.tools.core.util.CoreBundle.message
import com.intellij.openapi.components.ServiceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

const val KUBECTL_FAIL = "kubectl.fail"
const val METADATA_ERROR_MESSAGE_KEY = "error.message"

/**
 * This service builds and launches kubectl process from the flags and configurations passed
 * from [KubectlExecutorSettings].
 */
class KubectlExecutorService {
    companion object {
        val instance
            get() = ServiceManager.getService(KubectlExecutorService::class.java)!!
    }

    var kubectlExecutablePath: String = "kubectl"

    /**
     * run kubectl version to make sure that kubectl is available, if there's no response after
     *  a couple of seconds then it is not available.
     */
    fun isKubectlAvailable(): Boolean =
        try {
            val process = runKubectlCommand(KubectlExecutorSettings.ExecutionMode.VERSION.modeFlag)
            process.waitFor(1, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }

    /**
     * Check if kubectl is available, Check if the flags supplied are valid. Create and start the
     * command line process passed in. This method waits until the process has been completed and
     * needs to be run in a separate background thread.
     *
     * @param executorSettings : [KubectlExecutorSettings] that contain the mode
     *      and supporting arguments
     * @return a string containing the output of the command that was run
     */
    fun getKubectlOutputBlocking(executorSettings: KubectlExecutorSettings): String {

        if (!isKubectlAvailable()) {
            throw ExecutionException(message("kubectl.not.on.system.error"))
        }

        val command = concatArgs(executorSettings)

        val executingProcess = runKubectlCommand(command)

        executingProcess.waitFor()

        if (executingProcess.exitValue() == 0 ) {
            return processOutputToString(executingProcess)
        } else {
            throw ExecutionException(message("kubectl.unknown.error"))
        }
    }

    /**
     * Puts arguments together
     */
    @VisibleForTesting
    fun concatArgs(executorSettings: KubectlExecutorSettings): String {
        val commandList = mutableListOf<String>()

        with(commandList) {
            add(kubectlExecutablePath)
            add(executorSettings.executionMode.modeFlag)
            executorSettings.executionFlags.forEach {
                add(it)
            }
        }
        return commandList.joinToString(" ")
    }

    /**
     * Parse through command line arguments and try to execute the command
     */
    @VisibleForTesting
    fun runKubectlCommand(commandArgs: String ): Process {

        try {
            val processBuilder = ProcessBuilder()
            return processBuilder.command(commandArgs).start()
        } catch (e: Exception) {
            val kubectlFailEventName = KUBECTL_FAIL

            UsageTrackerService.getInstance().trackEvent(kubectlFailEventName)
                    .addMetadata(
                            METADATA_ERROR_MESSAGE_KEY, e.javaClass.name
                    ).ping()
            throw e
        }
    }

    /**
     * Check if kubectl is available, Check if the flags supplied are valid. Create and start the
     * command line process passed in
     */
    @VisibleForTesting
    fun processOutputToString(executingProcess: Process): String {
        val reader = BufferedReader(InputStreamReader(executingProcess.inputStream))
        val builder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            builder.append(line)
            line = reader.readLine()
            //so that there's no trailing or leading whitespace
            if (line != null) builder.append(System.getProperty("line.separator"))
        }
        return builder.toString()
    }
}

/**
 * Data object with launched Kubectl process and its command line.
 *
 * @property process System process for Kubectl.
 * @property commandLine Command line used to launch the process.
 */
data class KubectlProcess(val process: Process, val commandLine: String)

data class KubectlExecutorSettings(
    val executionMode: ExecutionMode,
    val executionFlags: List<String> = ArrayList()
) {

    /** Execution mode for Kubectl, single run, continuous development, etc. */
    enum class ExecutionMode(val modeFlag: String) {
        CONFIG("config"),
        CREATE("create"),
        VERSION("version")
    }
}
