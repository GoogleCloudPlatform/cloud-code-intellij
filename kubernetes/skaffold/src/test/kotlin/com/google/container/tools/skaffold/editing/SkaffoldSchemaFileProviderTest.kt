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

import com.google.common.truth.Truth.assertThat
import com.google.container.tools.skaffold.SkaffoldFileService
import com.google.container.tools.test.ContainerToolsRule
import com.google.container.tools.test.TestService
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [SkaffoldSchemaFileProvider].
 */
class SkaffoldSchemaFileProviderTest {

    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    private lateinit var skaffoldSchemaFileProvider: SkaffoldSchemaFileProvider

    @MockK
    @TestService
    private lateinit var skaffoldFileService: SkaffoldFileService

    @MockK
    private lateinit var virtualFile: VirtualFile

    private val SKAFFOLD_VERSION = "v1beta1"

    @Before
    fun setUp() {
        skaffoldSchemaFileProvider = SkaffoldSchemaFileProvider(SKAFFOLD_VERSION)
    }

    @Test
    fun `isAvailable returns true when valid skaffold file and matching schema`() {
        every { skaffoldFileService.getSkaffoldVersion(any()) } answers { SKAFFOLD_VERSION }
        every { skaffoldFileService.isSkaffoldFile(any()) } answers { true }

        assertThat(skaffoldSchemaFileProvider.isAvailable(virtualFile)).isTrue()
    }

    @Test
    fun `isAvailable returns false when invalid skaffold file`() {
        every { skaffoldFileService.getSkaffoldVersion(any()) } answers { SKAFFOLD_VERSION }
        every { skaffoldFileService.isSkaffoldFile(any()) } answers { false }

        assertThat(skaffoldSchemaFileProvider.isAvailable(virtualFile)).isFalse()
    }

    @Test
    fun `isAvailable returns false when not matching skaffold schema`() {
        every { skaffoldFileService.getSkaffoldVersion(any()) } answers { "v1alpha1" }
        every { skaffoldFileService.isSkaffoldFile(any()) } answers { true }

        assertThat(skaffoldSchemaFileProvider.isAvailable(virtualFile)).isFalse()
    }

    @Test
    fun `isAvailable returns false when skaffold version returns null`() {
        every { skaffoldFileService.getSkaffoldVersion(any()) } answers { null }
        every { skaffoldFileService.isSkaffoldFile(any()) } answers { true }

        assertThat(skaffoldSchemaFileProvider.isAvailable(virtualFile)).isFalse()
    }
}
