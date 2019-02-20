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
import com.google.container.tools.core.PluginInfo
import com.intellij.openapi.application.ApplicationInfo

const val IDE_LABEL = "ide"
const val IDE_VERSION_LABEL = "ideVersion"
const val PLUGIN_VERSION_LABEL = "ijPluginVersion"

/**
 * Maintains list of Kubernetes labels - string based key-value pairs used by Skaffold to apply
 * to all deployments. [getDefaultLabels] function supplies default set of labels used for all
 * deployments.
 */
class SkaffoldLabels {
    companion object {
        /**
         * Default set of labels for all Skaffold-based deployments. Includes IDE type and
         * version, plugin version.
         */
        val defaultLabels: SkaffoldLabels by lazy {
            populateDefaultLabels()
        }

        @VisibleForTesting
        fun populateDefaultLabels(): SkaffoldLabels {
            return SkaffoldLabels().apply {
                this.labels[IDE_LABEL] = PluginInfo.instance.platformPrefix
                this.labels[IDE_VERSION_LABEL] = ApplicationInfo.getInstance().getStrictVersion()
                this.labels[PLUGIN_VERSION_LABEL] = PluginInfo.instance.pluginVersion
            }
        }
    }

    val labels: MutableMap<String, String> = mutableMapOf()
}
