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

package com.google.container.tools.core.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.ServiceManager

private const val SKAFFOLD_EXECUTABLE_PATH_KEY = "SKAFFOLD_EXECUTABLE_PATH"

/**
 * An application service backed by a persistance [PropertiesComponent] used to store values from
 * the Kubernetes settings menu.
 */
class KubernetesSettingsService {
    companion object {
        val instance: KubernetesSettingsService
            get() = ServiceManager.getService(KubernetesSettingsService::class.java)!!
    }

    private val propertiesComponent = PropertiesComponent.getInstance()

    /**
     * Getter and setter for the path to the Skaffold executable. Stores and retrieves the value
     * from the persisted [PropertiesComponent].
     */
    var skaffoldExecutablePath: String
        get() = propertiesComponent.getValue(SKAFFOLD_EXECUTABLE_PATH_KEY) ?: ""
        set(path) {
            propertiesComponent.setValue(SKAFFOLD_EXECUTABLE_PATH_KEY, path)
        }
}