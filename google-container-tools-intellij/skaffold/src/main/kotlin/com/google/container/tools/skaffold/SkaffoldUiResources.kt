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
import com.intellij.CommonBundle
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.PropertyKey
import java.util.ResourceBundle

internal val SKAFFOLD_ICON = IconLoader.getIcon("/icons/skaffold.png")

private const val BUNDLE_NAME = "messages.SkaffoldBundle"

/**
 * Returns message by provided key from Skaffold message bundle with optional parameters.
 */
@VisibleForTesting
fun message(
    @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
    vararg params: String
): String {
    return CommonBundle.message(ResourceBundle.getBundle(BUNDLE_NAME), key, params)
}
