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

import com.intellij.openapi.vfs.VirtualFile
import org.yaml.snakeyaml.Yaml
import java.io.ByteArrayInputStream

/**
 * Skaffold YAML configuration parsed into a map of objects. Useful objects and names, such
 * as [profiles], are additionally parsed and presented in a structured format.
 * Malformed YAML files are not handled and exception ([ScannerException]/[IOException]) is
 * thrown for the caller to handle.
 */
class SkaffoldYamlConfiguration(skaffoldYamlFile: VirtualFile) {
    /** Map of objects (presented as maps/lists) to their names from YAML file. */
    private val skaffoldYamlMap = mutableMapOf<Any, Any>()

    init {
        val yamlLoader = Yaml()
        skaffoldYamlMap.putAll(
            yamlLoader.load(ByteArrayInputStream(skaffoldYamlFile.contentsToByteArray()))
        )
    }

    /** apiVersion of Skaffold configuration file, in the form of skaffold/v{number} */
    val apiVersion: String? = skaffoldYamlMap["apiVersion"]?.toString()

    /**
     * Skaffold profiles: map of profile name to a list of profile objects, each represented by
     * a map. If there are no profiles, empty map is returned.
     * For example, the following profile section:
     * ```
     * profiles:
     * - name: localImage
     *   build:
     *   artifact:
     *    - image: docker.io/local-image
     * - name: gcb
     *   build:
     *     googleCloudBuild:
     *        projectId: k8s-skaffold
     * ```
     * Returns map of two profiles {"localImage" -> profile data, "gcb" -> profile data}
     */
    val profiles: Map<String, Any>
        get() {
            val profilesMap = mutableMapOf<String, Any>()
            if (skaffoldYamlMap["profiles"] is List<*>) {
                (skaffoldYamlMap["profiles"] as List<*>).forEach { profileObject ->
                    if (profileObject is Map<*, *>) {
                        profileObject["name"]?.let { profileName ->
                            profilesMap[profileName.toString()] = profileObject
                        }
                    }
                }
            }

            return profilesMap.toMap()
        }
}
