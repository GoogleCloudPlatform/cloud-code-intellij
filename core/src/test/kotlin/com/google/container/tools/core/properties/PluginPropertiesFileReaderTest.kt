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

import com.google.common.truth.Truth
import org.junit.Test
import kotlin.test.fail

/**
 * Tests for [PluginPropertiesFileReader].
 */
class PluginPropertiesFileReaderTest {

    private val propertiesFileReader = PluginPropertiesFileReader("/test-config.properties")

    @Test
    fun `existing properties flag returns value`() {
        Truth.assertThat(propertiesFileReader.getPropertyValue("test.prop"))
            .isEqualTo("testPropValue")
    }

    @Test
    fun `non existing properties flag returns null`() {
        Truth.assertThat(propertiesFileReader.getPropertyValue("test.missing.prop")).isNull()
    }

    @Test
    fun `empty properties flag returns empty string`() {
        Truth.assertThat(propertiesFileReader.getPropertyValue("test.empty.prop")).isEmpty()
    }

    @Test
    fun `invalid config path throws exception`() {
        try {
            PluginPropertiesFileReader("/invalid/path")
            fail("Exception expected")
        } catch (ex: Exception) {
            // do nothing, exception expected
        }
    }
}
