/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.container.tools.test

/**
 * Indicates that the annotated field should replace the registered service in the application's
 * [PicoContainer][org.picocontainer.PicoContainer].
 *
 *
 * This is often used in conjunction with the [Mockk][io.mockk.mockk] annotation to replace
 * the real service with a mock. [ContainerToolsRule] handles the set-up and tear-down involved
 * for these services. For example, this is all that is required for a mocked `XxxService`
 * to substitute the real service in the [PicoContainer][org.picocontainer.PicoContainer]:
 *
 * ```
 * @get:Rule val  ContainerToolsRule rule = new ContainerToolsRule(this)
 *
 * @TestService @MockK mockXxxService: XxxService
 * ```
 *
 * Now this mock can be used like any other Mockk variable:
 *
 * ```
 * every { mockXxxService.getData() } returns listOf()
 * ```
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestService
