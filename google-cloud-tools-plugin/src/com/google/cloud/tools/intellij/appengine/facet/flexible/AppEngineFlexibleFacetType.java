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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

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

/**
 * The Flexible facet type.
 */
public class AppEngineFlexibleFacetType extends
    FacetType<AppEngineFlexibleFacet, AppEngineFlexibleFacetConfiguration> {

  public static final FacetTypeId<AppEngineFlexibleFacet> ID =
      new FacetTypeId<>("appEngineFlexible");
  private static final String STRING_ID = "app-engine-flexible";

  public AppEngineFlexibleFacetType() {
    super(ID, STRING_ID, GctBundle.getString("appengine.flexible.facet.name"));
  }

  @Override
  public AppEngineFlexibleFacetConfiguration createDefaultConfiguration() {
    return new AppEngineFlexibleFacetConfiguration();
  }

  @Override
  public AppEngineFlexibleFacet createFacet(@NotNull Module module, String name,
      @NotNull AppEngineFlexibleFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
    return new AppEngineFlexibleFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }
}
