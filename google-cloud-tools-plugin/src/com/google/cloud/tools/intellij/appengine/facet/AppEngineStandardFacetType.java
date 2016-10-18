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

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * @author nik
 */
public class AppEngineStandardFacetType extends FacetType<AppEngineStandardFacet,  AppEngineFacetConfiguration> {
  public static final String STRING_ID = "app-engine-standard";

  public AppEngineStandardFacetType() {
    super(AppEngineStandardFacet.ID, STRING_ID, GctBundle.message("appengine.standard.facet.name"));
  }

  public AppEngineFacetConfiguration createDefaultConfiguration() {
    return new AppEngineFacetConfiguration();
  }

  public AppEngineStandardFacet createFacet(@NotNull Module module,
                                    String name,
                                    @NotNull AppEngineFacetConfiguration configuration,
                                    @Nullable Facet underlyingFacet) {
    return new AppEngineStandardFacet(this, module, name, configuration);
  }

  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @NotNull
  @Override
  public String getDefaultFacetName() {
    return "Google App Engine Standard";
  }

  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }
}
