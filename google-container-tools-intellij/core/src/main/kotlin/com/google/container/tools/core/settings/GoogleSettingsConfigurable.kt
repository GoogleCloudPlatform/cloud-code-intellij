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

import com.google.container.tools.core.util.CoreBundle
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

/**
 * Configures the "Google" IDE settings top-level menu item.
 *
 * Multiple Google plugins may attempt to provide this menu item. The platform will ensure that only
 * one is created as long as the IDs match - as configured in the plugin xml settings.
 */
class GoogleSettingsConfigurable : Configurable {

    override fun getDisplayName(): String = CoreBundle.message("settings.menu.item.google.text")

    override fun isModified(): Boolean = false

    override fun apply() {}

    override fun createComponent(): JComponent? = null
}
