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

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.util.PlatformUtils

const val CONTAINER_TOOLS_PLUGIN_ID = "com.google.container.tools"
const val UNKNOWN_VERSION = "unknown"

/** Utilities to get common plugin information such as its version. */
class PluginInfo {
    companion object {
        val instance: PluginInfo
            get() = ServiceManager.getService(PluginInfo::class.java)!!
    }

    /** Version of the plugin installed, or `N/A` if version cannot be determined. */
    val pluginVersion: String by lazy {
        val ideaPluginDescriptor: IdeaPluginDescriptor? =
            PluginManager.getPlugin(PluginId.getId(CONTAINER_TOOLS_PLUGIN_ID))

        ideaPluginDescriptor?.version ?: UNKNOWN_VERSION
    }

    val platformPrefix: String by lazy { PlatformUtils.getPlatformPrefix() }
}
