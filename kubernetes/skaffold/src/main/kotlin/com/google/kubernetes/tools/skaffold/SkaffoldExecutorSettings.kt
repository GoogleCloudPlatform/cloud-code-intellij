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

import com.google.kubernetes.tools.skaffold.SkaffoldExecutorSettings.ExecutionMode
import java.io.File

/**
 * Set of settings to control Skaffold execution, including flags and execution mode.
 * Pass these settings into [SkaffoldExecutorService].
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
 * @property analyzeOnInit Disables interactive mode for `init` and prints list of found build and
 *           deploy artifacts
 * @property forceInit Disables interactive mode for `init` and forces creation of skaffold.yaml
 *           using either default or specified artifacts.
 */
data class SkaffoldExecutorSettings(
    val executionMode: ExecutionMode,
    val skaffoldConfigurationFilePath: String? = null,
    val skaffoldProfile: String? = null,
    val workingDirectory: File? = null,
    val skaffoldLabels: SkaffoldLabels? = null,
    val tailLogsAfterDeploy: Boolean? = null,
    val defaultImageRepo: String? = null,
    val analyzeOnInit: Boolean = false,
    val forceInit: Boolean = false
) {

    /** Execution mode for Skaffold, single run, continuous development (run or debug mode), etc. */
    enum class ExecutionMode(val modeFlag: String) {
        SINGLE_RUN("run"),
        DEV("dev"),
        DEBUG("debug"),
        INIT("init"),
        VERSION("version")
    }
}
