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

package com.google.container.tools.core

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat


import com.google.kubernetes.tools.core.util.CoreBundle
import com.google.kubernetes.tools.test.ContainerToolsRule
import com.google.kubernetes.tools.test.TestFile
import com.google.kubernetes.tools.test.TestService
import com.google.kubernetes.tools.test.expectThrows
import com.google.kubernetes.tools.core.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.SearchScopeProvider
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.junit.Before
import org.junit.Rule
import org.junit.Test


/** Unit tests for [KubectlAPI] */
class KubectlAPITest {
    @get:Rule
    val containerToolsRule = ContainerToolsRule(this)

    @MockK
    @TestService
    private lateinit var mockKubectlExecutorService: KubectlExecutorService

//    @MockK
    private lateinit var mockKubectlAPI: KubectlAPI

//    private val kubectlSettingsCapturingSlot: CapturingSlot<KubectlExecutorSettings> = slot()

    @Before
    fun setUp() {
        every { mockKubectlExecutorService.isKubectlAvailable() } answers {true}

        mockKubectlAPI = KubectlAPI()

        // Kubectl executor answer mocks
        val mockKubectlProcess: KubectlProcess = mockk(relaxed = true)
//        every {
//            mockKubectlExecutorService.executeKubectl(capture(kubectlSettingsCapturingSlot))
//        } answers { mockKubectlProcess }
    }

    @Test
    fun `startProcess with version returns the version`() {
        val result = mockKubectlAPI.startProcess(
                KubectlExecutorSettings.ExecutionMode.VERSION,
                listOf())
        print(result)
        Truth.assertThat(true).isFalse()
//        Truth.assertThat(result).isEqualTo("kubectl config")
    }

    @Test
    fun `configGetClusters returns the clusters`() {
        val result = mockKubectlAPI.startProcess(
                KubectlExecutorSettings.ExecutionMode.VERSION,
                listOf()).toString()

        Truth.assertThat(result).isEqualTo("kubectl config")
    }

    @Test
    fun `configSetCluster sets the cluster context`() {
        val result = mockKubectlAPI.startProcess(
                KubectlExecutorSettings.ExecutionMode.VERSION,
                listOf()).toString()

        Truth.assertThat(result).isEqualTo("kubectl config")
    }

    @Test
    fun `configSetCluster fails on invalid name output`() {
        val result = mockKubectlAPI.startProcess(
                KubectlExecutorSettings.ExecutionMode.VERSION,
                listOf()).toString()

        Truth.assertThat(result).isEqualTo("kubectl config")
    }

}