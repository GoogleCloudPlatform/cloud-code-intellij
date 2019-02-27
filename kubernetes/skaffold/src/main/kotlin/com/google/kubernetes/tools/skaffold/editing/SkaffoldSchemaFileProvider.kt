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

package com.google.kubernetes.tools.skaffold.editing

import com.google.kubernetes.tools.skaffold.SkaffoldFileService
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType

const val SKAFFOLD_SCHEMA_NAME = "skaffold"

/**
 * A [JsonSchemaFileProvider] that attaches a bundled JSON schema to the editor corresponding to the
 * version of Skaffold.
 */
class SkaffoldSchemaFileProvider(private val skaffoldSchemaVersion: String)
    : JsonSchemaFileProvider {

    override fun getName(): String = SKAFFOLD_SCHEMA_NAME

    /**
     * Returns true if the currently open file is a valid Skaffold file, and the Skaffold version
     * associated with this schema provider matches the Skaffold version in the currently open file.
     */
    override fun isAvailable(file: VirtualFile): Boolean =
            SkaffoldFileService.instance.isSkaffoldFile(file) &&
                    SkaffoldFileService.instance.getSkaffoldVersion(file) == skaffoldSchemaVersion

    override fun getSchemaFile(): VirtualFile? {
        return JsonSchemaProviderFactory
                .getResourceFile(this::class.java, "/schemas/$skaffoldSchemaVersion.json")
    }

    override fun getSchemaType(): SchemaType {
        return SchemaType.embeddedSchema
    }
}
