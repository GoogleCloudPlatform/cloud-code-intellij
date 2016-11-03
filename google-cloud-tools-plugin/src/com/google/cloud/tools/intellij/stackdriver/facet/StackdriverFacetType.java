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

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class StackdriverFacetType
    extends FacetType<StackdriverFacet, StackdriverFacetConfiguration> {
  public static final String STRING_ID = "stackdriver";
  public static final FacetTypeId<StackdriverFacet> ID = new FacetTypeId<>(STRING_ID);

  public StackdriverFacetType() {
    super(ID, STRING_ID, GctBundle.getString("stackdriver.name"));
  }

  @Override
  public StackdriverFacetConfiguration createDefaultConfiguration() {
    return new StackdriverFacetConfiguration();
  }

  @Override
  public StackdriverFacet createFacet(@NotNull Module module, String name,
      @NotNull StackdriverFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
    return new StackdriverFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    // TODO(joaomartins): Replace with the generic green Stackdriver logo.
    return GoogleCloudToolsIcons.STACKDRIVER_DEBUGGER;
  }
}
