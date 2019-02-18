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

package com.google.container.tools.test

/**
 * Indicates that the annotated test method should be run on UI event dispatch thread (EDT).
 * This annotation should be used for all unit tests that manipulate or create any Java Swing UI
 * components or their children or other UI state, including all IDE platform components.
 *
 * This is used in conjunction with the [@Test][org.junit.Test] annotation to run the test method
 * on EDT.
 *
 * ```
 * @Test
 * @UiTest
 * fun manipulates_UI_components() { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UiTest
