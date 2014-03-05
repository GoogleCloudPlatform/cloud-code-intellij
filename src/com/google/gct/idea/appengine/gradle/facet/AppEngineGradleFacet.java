/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.google.gct.idea.appengine.gradle.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * App Engine Gradle facet for App Engine Modules with a Gradle build file
 */
public class AppEngineGradleFacet extends Facet<AppEngineGradleFacetConfiguration> {
  private static final Logger LOG = Logger.getInstance(AppEngineGradleFacet.class);

  @NonNls public static final String ID = "app-engine-gradle";
  @NonNls public static final String NAME = "App Engine Gradle";

  public static final FacetTypeId<AppEngineGradleFacet> TYPE_ID = new FacetTypeId<AppEngineGradleFacet>(ID);

  // Need to modify this to reference the Model for an AppEngine Gradle project
  // private IdeaGradleProject myGradleProject;

  @Nullable
  public static AppEngineGradleFacet getInstance(@NotNull Module module) {
    return FacetManager.getInstance(module).getFacetByType(TYPE_ID);
  }

  @SuppressWarnings("ConstantConditions")
  public AppEngineGradleFacet(@NotNull FacetType facetType,
                              @NotNull Module module,
                              @NotNull String name,
                              @NotNull AppEngineGradleFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);
  }

  public static FacetType<AppEngineGradleFacet, AppEngineGradleFacetConfiguration> getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(ID);
  }

  @Nullable
  public static AppEngineGradleFacet getAppEngineFacetByModule(@Nullable Module module) {
    if (module == null) return null;
    return FacetManager.getInstance(module).getFacetByType(TYPE_ID);
  }
}
