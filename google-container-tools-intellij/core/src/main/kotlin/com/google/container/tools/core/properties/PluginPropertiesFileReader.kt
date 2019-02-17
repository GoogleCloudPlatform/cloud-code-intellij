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

package com.google.container.tools.core.properties

import com.intellij.openapi.components.ServiceManager
import java.util.Properties

/**
 * Reads property values from the specified properties file supplied to the plugin.
 *
 * @param propertyFilePath the path to the properties file; defaults to 'config.properties' located
 * in the root of the module's resources
 */
class PluginPropertiesFileReader(
    propertyFilePath: String = DEFAULT_PROPERTIES_FILE_NAME
) {

    private val properties: Properties = Properties()

    companion object {
        private const val DEFAULT_PROPERTIES_FILE_NAME = "/config.properties"

        val instance
            get() = ServiceManager.getService(PluginPropertiesFileReader::class.java)!!
    }

    init {
        this.javaClass.getResourceAsStream(propertyFilePath)?.use {
            properties.load(it)
        } ?: throw IllegalArgumentException(
            "Failed to load plugin property configuration file: $propertyFilePath"
        )
    }

    /**
     * Return the property value given the passed in name.
     */
    fun getPropertyValue(propertyKey: String): String? = properties.getProperty(propertyKey)
}
