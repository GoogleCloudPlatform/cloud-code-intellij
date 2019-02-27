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

package com.google.container.tools.skaffold.editing

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory

/**
 * A Skaffold [JsonSchemaProviderFactory] that returns a the providers for each schema corresponding
 * to our supported Skaffold versions.
 */
class SkaffoldSchemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): MutableList<JsonSchemaFileProvider> {
        return mutableListOf(
                SkaffoldSchemaFileProvider("v1beta6"),
                SkaffoldSchemaFileProvider("v1beta5"),
                SkaffoldSchemaFileProvider("v1beta4"),
                SkaffoldSchemaFileProvider("v1beta3"),
                SkaffoldSchemaFileProvider("v1beta2"),
                SkaffoldSchemaFileProvider("v1beta1"),
                SkaffoldSchemaFileProvider("v1alpha5"),
                SkaffoldSchemaFileProvider("v1alpha4"),
                SkaffoldSchemaFileProvider("v1alpha3"),
                SkaffoldSchemaFileProvider("v1alpha2"),
                SkaffoldSchemaFileProvider("v1alpha1")
        )
    }
}
