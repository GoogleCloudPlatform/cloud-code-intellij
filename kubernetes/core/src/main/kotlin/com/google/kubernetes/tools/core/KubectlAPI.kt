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

import com.google.common.annotations.VisibleForTesting
import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.ServiceManager
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit


/**
 * Runs Kubectl on command line in both "run"/"dev" modes based on the given
 *
 */
class KubectlAPI {
    fun configGetClusters(): String{
        val configurationFlags = listOf("get-clusters")
        val executionMode = KubectlExecutorSettings.ExecutionMode.CONFIG
        val process = startProcess(executionMode, configurationFlags)
        return if (!process.isNullOrBlank()) process else ""
    }

    fun configSetCluster(name:String): Boolean { //use-context to set the active context
        val configurationFlags = listOf("set-cluster", name)
        val executionMode = KubectlExecutorSettings.ExecutionMode.CONFIG
        val process = startProcess(executionMode, configurationFlags)
        return (!process.isNullOrBlank())
    }


    fun startProcess(
            executionMode: KubectlExecutorSettings.ExecutionMode,
            configurationFlags:List<String> ): String? {
        // ensure the configuration is valid for execution - settings are of supported type,


//        throw RuntimeException("wtf")
        try {

            if (!KubectlExecutorService.instance.isKubectlAvailable()) {
                throw ExecutionException(message("kubectl.not.on.system.error"))
            }

            val n1 =KubectlExecutorSettings(
                    executionMode = executionMode,
                    executionFlags = configurationFlags
            )

            val n2 = KubectlExecutorService.instance.executeKubectl(n1)
            val executingProcess =  n2.process

            executingProcess.waitFor(2, TimeUnit.SECONDS)

            if(executingProcess.exitValue() == 0 ) {
                val reader = BufferedReader(InputStreamReader(executingProcess.inputStream))
                var builder = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    builder.append(line)
                    builder.append(System.getProperty("line.separator"))
                }
                return builder.toString()


//                ProcessBuilder builder = new ProcessBuilder("command goes here");
//                builder.redirectErrorStream(true);
//                Process process = builder.start();
//                InputStream is = process.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            } else {
                throw ExecutionException(message("kubectl.invalid.flags.error"))
            }

        } catch (e: Exception){
            throw ExecutionException(e)
            return null
        }
    }

}
