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
        val process = KubectlExecutorService.instance.startProcess(executionMode, configurationFlags)
        return if (!process.isNullOrBlank()) process else ""
    }

    fun configSetCluster(name:String): Boolean { //use-context to set the active context
        val configurationFlags = listOf("set-cluster", name)
        val executionMode = KubectlExecutorSettings.ExecutionMode.CONFIG
        val process = KubectlExecutorService.instance.startProcess(executionMode, configurationFlags)
        return (!process.isNullOrBlank())
    }




}
