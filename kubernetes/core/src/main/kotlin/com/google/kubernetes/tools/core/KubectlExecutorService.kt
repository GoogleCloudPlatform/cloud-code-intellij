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

package com.google.kubernetes.tools.core

import com.google.cloud.tools.intellij.analytics.UsageTrackerService
import com.intellij.openapi.components.ServiceManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

const val KUBECTL_SUCCESS = "kubectl.run"
const val KUBECTL_FAIL = "kubectl.fail"
const val METADATA_ERROR_MESSAGE_KEY = "error.message"

class KubectlExecutorService {
    companion object {
        val instance
            get() = ServiceManager.getService(KubectlExecutorService::class.java)!!
    }


    var kubectlExecutablePath: String = "kubectl"

    fun isKubectlAvailable():Boolean=
        try {
            val settings = KubectlExecutorSettings(KubectlExecutorSettings.ExecutionMode.VERSION)
            val process = executeKubectl(settings).process
            process.waitFor(2, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }


    fun createProcess(
            workingDirectory: File?,
            commandList: List<String>
    ): Process {
        val processBuilder = ProcessBuilder()
        workingDirectory?.let { processBuilder.directory(it) }

        return processBuilder.command(commandList).start()
    }

    fun executeKubectl(settings: KubectlExecutorSettings): KubectlProcess {
        val commandList = mutableListOf<String>()
        with(commandList) {
            add(kubectlExecutablePath)
            add(settings.executionMode.modeFlag)
            settings.executionFlags?.forEach {
                add(it)
            }
        }

        try {
            val kubectlProcess = KubectlProcess(
                    createProcess(settings.workingDirectory, commandList),
                    commandLine = commandList.joinToString(" ")
            )

            // track event based on execution mode.

            UsageTrackerService.getInstance().trackEvent(KUBECTL_SUCCESS).ping()
            return kubectlProcess
        } catch (e: Exception) {
            val skaffoldFailEventName = KUBECTL_FAIL

            UsageTrackerService.getInstance().trackEvent(skaffoldFailEventName)
                    .addMetadata(
                            METADATA_ERROR_MESSAGE_KEY, e.javaClass.name
                    ).ping()
            // re-throw exception to make sure a user sees run resulted in an error and sees error
            // message.
            throw e
        }
    }

    fun startProcess(
            executionMode: KubectlExecutorSettings.ExecutionMode,
            configurationFlags:List<String> ): String? {
        // ensure the configuration is valid for execution - settings are of supported type,

        try {

            if (!isKubectlAvailable()) {
                throw Exception(message("kubectl.not.on.system.error"))
            }

            val n1 =KubectlExecutorSettings(
                    executionMode = executionMode,
                    executionFlags = configurationFlags
            )

            val n2 = executeKubectl(n1)
            val executingProcess =  n2.process

            executingProcess.waitFor(2, TimeUnit.SECONDS)

            if(executingProcess.exitValue() == 0 ) {
                val reader = BufferedReader(InputStreamReader(executingProcess.inputStream))
                var builder = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    System.out.println(line)
                    builder.append(line)
                    builder.append(System.getProperty("line.separator"))
                    line = reader.readLine()
                }
                return builder.toString()
            } else {
                throw com.intellij.execution.ExecutionException(message("kubectl.invalid.flags.error"))
            }

        } catch (e: Exception){
            throw com.intellij.execution.ExecutionException(e)
            return null
        }
    }
}



/**
 * Data object with launched Skaffold process and its command line.
 *
 * @property process System process for Skaffold.
 * @property commandLine Command line used to launch the process.
 */
data class KubectlProcess(val process: Process, val commandLine: String)

data class KubectlExecutorSettings(
        val executionMode: ExecutionMode,
        val workingDirectory: File? = null,
        val executionFlags: List<String> = ArrayList()
) {

    /** Execution mode for Skaffold, single run, continuous development, etc. */
    enum class ExecutionMode(val modeFlag: String) {
        CONFIG("config"),
        CREATE("create"),
        GET("get"),
        PATCH("patch"),
        ROLLOUT("rollout"),
        SCALE("scale"),
        DELETE("delete"),
        RUN("run"),
        VERSION("version")
    }
}

