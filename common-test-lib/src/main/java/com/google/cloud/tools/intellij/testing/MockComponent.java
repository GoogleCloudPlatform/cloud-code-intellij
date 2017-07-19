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
 * Indicates that the annotated field is a mocked value that should replace the registered component
 * in the application's {@link org.picocontainer.PicoContainer PicoContainer}.
 *
 * <p>{@link CloudToolsRule} handles the set-up and tear-down involved for these mocks. For example,
 * this is all that is required for a mocked {@code CloudSdkService} to substitute the real
 * component in the {@link org.picocontainer.PicoContainer PicoContainer}:
 *
 * <pre>
 *   {@literal @Rule} public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
 *
 *   {@literal @Mock @MockComponent} private CloudSdkService mockCloudSdkService;
 * </pre>
 *
 * <p>Now this mock can be used like any other Mockito variable:
 *
 * <pre>
 *   when(mockCloudSdkService.validateCloudSdk()).thenReturn(ImmutableSet.of());
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockComponent {}
