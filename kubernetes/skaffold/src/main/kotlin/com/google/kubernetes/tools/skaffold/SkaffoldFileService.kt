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

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.YAMLFileType

// see https://github.com/GoogleContainerTools/skaffold/blob/master/examples/annotated-skaffold.yaml
private const val SKAFFOLD_API_HEADER = "skaffold/v"

/** IDE service for finding Skaffold files in the given IDE project. */
class SkaffoldFileService {
    companion object {
        /** Current active implementation of [SkaffoldFileService] */
        val instance: SkaffoldFileService
            get() = ServiceManager.getService(SkaffoldFileService::class.java)!!
    }

    /**
     * Checks if a given file is a valid Skaffold configuration file based on type and API version.
     */
    fun isSkaffoldFile(file: VirtualFile): Boolean =
            try {
                isValidYamlFile(file) && isValidSkaffoldFile(SkaffoldYamlConfiguration(file))
            } catch (ex: Exception) {
                // We don't care about I/O or scan exceptions here since we only need to know if
                // the YAML file was in the proper format.
                false
            }

    /**
     * Returns the version of Skaffold as defined in skaffold.yaml, or null if it can't be read.
     *
     * The version is represented as skaffold/[version] in the yaml.
     */
    fun getSkaffoldVersion(file: VirtualFile): String? =
            try {
                if (isValidYamlFile(file)) {
                    val skaffoldYaml = SkaffoldYamlConfiguration(file)

                    if (isValidSkaffoldFile(skaffoldYaml)) {
                        skaffoldYaml.skaffoldVersion
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (ex: Exception) {
                // If version can't be read, return null
                null
            }

    /**
     * Finds all Skaffold configuration YAML files in the given project.
     *
     * @param project IDE project to search Skaffold file in
     * @return List of Skaffold configuration files in the project.
     */
    fun findSkaffoldFiles(project: Project): List<VirtualFile> =
            FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.allScope(project))
                    .filter { isSkaffoldFile(it) }

    private fun isValidYamlFile(file: VirtualFile): Boolean =
            with(file) {
                !isDirectory && fileType is YAMLFileType && isValid
            }

    /**
     * A file is a valid Skaffold file if it has the correct API header.
     */
    private fun isValidSkaffoldFile(skaffoldYamlConfiguration: SkaffoldYamlConfiguration): Boolean =
            skaffoldYamlConfiguration.apiVersion?.startsWith(SKAFFOLD_API_HEADER) == true
}
