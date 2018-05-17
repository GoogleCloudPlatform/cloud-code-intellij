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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineGradlePluginFacetType.AppEngineGradlePluginFacetConfiguration;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

/**
 * An App Engine Gradle plugin {@link Facet}. Indicates that the module has the app-gradle-plugin
 * configured. The facet stores information provided by the Gradle integration.
 */
public class AppEngineGradlePluginFacet extends Facet<AppEngineGradlePluginFacetConfiguration> {

  public AppEngineGradlePluginFacet(
      @NotNull FacetType facetType,
      @NotNull Module module,
      @NotNull String name,
      @NotNull AppEngineGradlePluginFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);
  }

  public static AppEngineGradlePluginFacet getInstance(Module module) {
    return FacetManager.getInstance(module).getFacetByType(AppEngineGradlePluginFacetType.ID);
  }

  /** Returns {@code true} if the supplied module has the {@link AppEngineGradlePluginFacet}. */
  public static boolean hasFacet(@NotNull Module module) {
    return FacetManager.getInstance(module).getFacetByType(AppEngineGradlePluginFacetType.ID)
        != null;
  }

  /** Returns the {@link AppEngineGradlePluginFacetType} associated with this facet. */
  public static FacetType<AppEngineGradlePluginFacet, AppEngineGradlePluginFacetConfiguration>
      getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(AppEngineGradlePluginFacetType.ID);
  }
}
