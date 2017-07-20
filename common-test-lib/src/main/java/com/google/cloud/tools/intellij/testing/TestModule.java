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
 * Marks a {@link com.intellij.openapi.module.Module Module} that should be created and added to the
 * test fixture's {@link com.intellij.openapi.project.Project Project}.
 *
 * <p>{@link CloudToolsRule} manages the creation and injection of this module. You can also specify
 * a value for {@link #facetTypeId()} to have the facet associated with the given ID added to the
 * module. For example:
 *
 * <pre>
 *   &#64;Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
 *
 *   &#64;TestModule(facetTypeId = MyFacetType.ID)
 *   private Module myModule;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestModule {

  /**
   * The ID of the {@link com.intellij.facet.FacetType FacetType} to add to the module.
   *
   * <p>Defaults to an empty string.
   */
  String facetTypeId() default "";
}
