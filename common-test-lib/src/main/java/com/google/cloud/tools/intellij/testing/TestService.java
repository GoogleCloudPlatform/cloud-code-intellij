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
 * Indicates that the annotated field should replace the registered service in the application's
 * {@link org.picocontainer.PicoContainer PicoContainer}.
 *
 * <p>This is often used in conjunction with the {@link org.mockito.Mock Mock} annotation to replace
 * the real service with a mock. {@link CloudToolsRule} handles the set-up and tear-down involved
 * for these services. For example, this is all that is required for a mocked {@code
 * CloudSdkService} to substitute the real service in the {@link org.picocontainer.PicoContainer
 * PicoContainer}:
 *
 * <pre>
 *   &#64;Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
 *
 *   &#64;Mock &#64;TestService private CloudSdkService mockCloudSdkService;
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
public @interface TestService {}
