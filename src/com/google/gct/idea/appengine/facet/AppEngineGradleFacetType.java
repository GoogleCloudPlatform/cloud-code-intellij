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
package com.google.gct.idea.appengine.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * The App Engine Gradle facet type for App Engine projects with a Gradle build file
 */
public class AppEngineGradleFacetType extends FacetType<AppEngineGradleFacet, AppEngineGradleFacetConfiguration> {

  public AppEngineGradleFacetType() {
    super(AppEngineGradleFacet.TYPE_ID, AppEngineGradleFacet.ID, AppEngineGradleFacet.NAME);
  }

  @Override
  public AppEngineGradleFacetConfiguration createDefaultConfiguration() {
    return new AppEngineGradleFacetConfiguration();
  }

  @Override
  public AppEngineGradleFacet createFacet(@NotNull Module module,
                                    String name,
                                    @NotNull AppEngineGradleFacetConfiguration configuration,
                                    @Nullable Facet underlyingFacet) {
    return new AppEngineGradleFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @NotNull
  @Override
  public String getDefaultFacetName() {
    return AppEngineGradleFacet.NAME;
  }

  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.AppEngine;
  }
}
