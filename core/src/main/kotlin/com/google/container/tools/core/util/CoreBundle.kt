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

package com.google.container.tools.core.util

import com.intellij.CommonBundle
import org.jetbrains.annotations.PropertyKey
import java.util.ResourceBundle

private const val BUNDLE_NAME = "messages.CoreBundle"

/**
 * Message bundle manager for core module.
 */
object CoreBundle {

    /**
     * Returns messages for the given key from the CoreBundle message bundle with optional
     * parameters.
     */
    fun message(
        @PropertyKey(resourceBundle = BUNDLE_NAME) key: String,
        vararg params: String
    ): String = CommonBundle.message(ResourceBundle.getBundle(BUNDLE_NAME), key, params)
}
