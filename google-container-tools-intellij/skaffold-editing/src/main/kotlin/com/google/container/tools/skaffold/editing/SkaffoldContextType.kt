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

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.YAMLFileType

private const val SKAFFOLD_TEMPLATE_ID = "SKAFFOLD"
private const val SKAFFOLD_TEMPLATE_NAME = "Skaffold"

/**
 * Defines the [TemplateContextType] for Skaffold files.
 */
class SkaffoldContextType(
    id: String = SKAFFOLD_TEMPLATE_ID,
    presentableName: String = SKAFFOLD_TEMPLATE_NAME
) :
    TemplateContextType(id, presentableName) {

    /**
     * A file is in context for Skaffold live templates if it is a yaml file.
     */
    override fun isInContext(file: PsiFile, offset: Int): Boolean =
        file.fileType == YAMLFileType.YML
}
