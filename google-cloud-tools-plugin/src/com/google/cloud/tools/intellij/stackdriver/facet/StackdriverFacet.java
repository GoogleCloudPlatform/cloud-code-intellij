/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

/**
 * Stores source context generation settings.
 */
public class StackdriverFacet extends Facet<StackdriverFacetConfiguration> {

  public StackdriverFacet(@NotNull FacetType facetType,
      @NotNull Module module,
      @NotNull String name,
      @NotNull StackdriverFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null /* underlyingFacet */);
    configuration.getState().setModuleSourceDirectory(
        Paths.get(module.getModuleFilePath()).getParent().toString());
  }
}
