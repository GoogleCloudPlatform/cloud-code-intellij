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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.yaml.YAMLFileType
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern

// see https://github.com/GoogleContainerTools/skaffold/blob/master/examples/annotated-skaffold.yaml
private const val SKAFFOLD_API_HEADER_REGEX = """\s*apiVersion:\s*skaffold/v"""
private val SKAFFOLD_API_HEADER_PATTERN: Pattern by lazy {
    Pattern.compile(SKAFFOLD_API_HEADER_REGEX)
}

/**
 * Checks if a given file is a valid Skaffold configuration file based on type and API version.
 */
internal fun isSkaffoldFile(file: VirtualFile): Boolean {
    with(file) {
        if (!isDirectory && fileType is YAMLFileType && isValid) {
            val inputStream: InputStream = ByteArrayInputStream(contentsToByteArray())
            inputStream.use {
                val scanner = Scanner(it)
                // consider this YAML file as Skaffold when first line contains proper API version
                return scanner.hasNextLine() &&
                    SKAFFOLD_API_HEADER_PATTERN.matcher(scanner.nextLine()).find()
            }
        }
    }
    return false
}

/**
 * Finds all Skaffold configuration YAML files in the given project.
 *
 * @param project IDE project to search Skaffold file in
 * @return List of Skaffold configuration files in the project.
 */
internal fun findSkaffoldFiles(project: Project): List<VirtualFile> {
    return FileTypeIndex.getFiles(YAMLFileType.YML, GlobalSearchScope.allScope(project))
        .filter { isSkaffoldFile(it) }
}
