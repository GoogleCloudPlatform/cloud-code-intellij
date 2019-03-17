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

import com.intellij.execution.ExecutionException
import com.google.cloud.tools.intellij.analytics.UsageTrackerService
import com.intellij.openapi.components.ServiceManager
import java.io.BufferedReader
import java.io.InputStreamReader
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

    /**
     * run kubectl version to make sure that kubectl is available, if there's no response after
     *  a couple of seconds then it is not available.
     */
    fun isKubectlAvailable():Boolean=
        try {
            val settings = KubectlExecutorSettings(KubectlExecutorSettings.ExecutionMode.VERSION)
            val process = executeKubectl(settings).process
            process.waitFor(2, TimeUnit.SECONDS)
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }


    /**
     * Parse through command line arguments and try to execute the command
     */
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
            val processBuilder = ProcessBuilder()
            val startedProcess = processBuilder.command(commandList).start()
            val kubectlProcess = KubectlProcess(
                    startedProcess,
                    commandList.joinToString(" ")
            )
            // track event based on execution mode.
            UsageTrackerService.getInstance().trackEvent(KUBECTL_SUCCESS).ping()
            return kubectlProcess

        } catch (e: Exception) {
            val kubectlFailEventName = KUBECTL_FAIL

            UsageTrackerService.getInstance().trackEvent(kubectlFailEventName)
                    .addMetadata(
                            METADATA_ERROR_MESSAGE_KEY, e.javaClass.name
                    ).ping()
            // re-throw exception to make sure a user sees run resulted in an error and sees error
            // message.
            throw e
        }
    }

    /**
     * Check if kubectl is available, Check if the flags supplied are valid. Create and start the
     * command line process passed in
     */

    fun processOutputToString(executingProcess: Process): String{
        val reader = BufferedReader(InputStreamReader(executingProcess.inputStream))
        var builder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            builder.append(line)
            line = reader.readLine()
            //so that there's no trailing or leading whitespace
            if (line != null) builder.append(System.getProperty("line.separator"))
        }
        return builder.toString()
    }

    /**
     * Check if kubectl is available, Check if the flags supplied are valid. Create and start the
     * command line process passed in
     */
    fun startProcess(
            executionMode: KubectlExecutorSettings.ExecutionMode,
            configurationFlags:List<String> ): String {

        if (!isKubectlAvailable()) {
            throw ExecutionException(message("kubectl.not.on.system.error"))
        }

        val executorSettings =KubectlExecutorSettings(
                executionMode = executionMode,
                executionFlags = configurationFlags
        )

        if (!executorSettings.hasValidFlags()){
            throw ExecutionException(message("kubectl.invalid.flags.error"))
        }

        val executingProcess =  executeKubectl(executorSettings).process

        executingProcess.waitFor(2, TimeUnit.SECONDS)

        if(executingProcess.exitValue() == 0 ) {
            return processOutputToString(executingProcess)
        } else {
            throw ExecutionException(message("kubectl.unknown.error"))
        }

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

    /** Map to check if flags are valid for an execution mode */
    private val acceptedFlagsMap = mapOf(
            "-f" to ExecutionMode.CREATE,
            "get-clusters" to ExecutionMode.CONFIG,
            "set-cluster" to ExecutionMode.CONFIG
    )

    /** Make sure the mode actually support the flags */
    fun hasValidFlags(): Boolean{
        var skip = false
        for (flag in executionFlags.listIterator()){
            //if it's a value, don't check it. We're only checking flags
            if (skip) {
                skip=false
                continue
            }

            if (flag.contains("=")){
                //don't skip the next argument
                val flagAndValue=flag.split("=")
                val justTheFlag= flagAndValue[0]
                val modeOfFlag = acceptedFlagsMap.get(justTheFlag)
                if (modeOfFlag != executionMode) return false

            } else {
                //skip the next argument
                val modeOfFlag = acceptedFlagsMap.get(flag)
                if (modeOfFlag != executionMode) return false
                skip = true
            }

        }
        return true
    }
}

