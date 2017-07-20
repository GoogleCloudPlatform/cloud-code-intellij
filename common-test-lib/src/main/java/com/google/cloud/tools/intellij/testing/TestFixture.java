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

package com.google.cloud.tools.intellij.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field where the {@link com.intellij.testFramework.fixtures.IdeaProjectTestFixture
 * IdeaProjectTestFixture}, created by {@link CloudToolsRule}, should be injected.
 *
 * <p>This is useful to gain access to the {@link com.intellij.openapi.project.Project Project}
 * inside of a unit test. For example:
 *
 * <pre>
 *   &#64;Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
 *
 *   &#64;TestFixture private IdeaProjectTestFixture testFixture;
 *
 *   &#64;Test
 *   public void myTest() {
 *     Project project = testFixture.getProject();
 *     // Do something with the project...
 *   }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestFixture {}
