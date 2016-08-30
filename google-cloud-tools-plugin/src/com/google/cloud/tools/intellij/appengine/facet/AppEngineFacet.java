/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.facet;

import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class AppEngineFacet extends Facet<AppEngineFacetConfiguration> {

  public static final FacetTypeId<AppEngineFacet> ID = new FacetTypeId<>("appEngine");

  public AppEngineFacet(@NotNull FacetType facetType,
      @NotNull Module module,
      @NotNull String name,
      @NotNull AppEngineFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_SUPPORT_ADDED)
        .ping();
  }

  public static FacetType<AppEngineFacet, AppEngineFacetConfiguration> getFacetType() {
    return FacetTypeRegistry.getInstance().findFacetType(ID);
  }

  @Nullable
  public static AppEngineFacet getAppEngineFacetByModule(@Nullable Module module) {
    if (module == null) {
      return null;
    }
    return FacetManager.getInstance(module).getFacetByType(ID);
  }

}
