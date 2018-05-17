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

import com.google.cloud.tools.intellij.appengine.java.AppEngineIcons;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineGradlePluginFacetType.AppEngineGradlePluginFacetConfiguration;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AppEngineGradlePluginFacetType
    extends FacetType<AppEngineGradlePluginFacet, AppEngineGradlePluginFacetConfiguration> {

  public static final FacetTypeId<AppEngineGradlePluginFacet> ID = new FacetTypeId<>("appEngineGradle");
  public static final String STRING_ID = "app-engine-gradle";
  public static final String NAME = "Google App Engine Gradle";

  public AppEngineGradlePluginFacetType() {
    super(ID, STRING_ID, NAME);
  }

  @Override
  public AppEngineGradlePluginFacetConfiguration createDefaultConfiguration() {
    return new AppEngineGradlePluginFacetConfiguration();
  }

  @Override
  public AppEngineGradlePluginFacet createFacet(
      @NotNull Module module,
      String name,
      @NotNull AppEngineGradlePluginFacetConfiguration configuration,
      @Nullable Facet underlyingFacet) {
    return new AppEngineGradlePluginFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }

  public static class AppEngineGradlePluginFacetConfiguration
      implements FacetConfiguration, PersistentStateComponent<AppEngineGradlePluginFacetConfiguration> {

    private String gradleBuildDir;

    @Override
    public FacetEditorTab[] createEditorTabs(
        FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
      return new FacetEditorTab[0];
    }

    @Nullable
    @Override
    public AppEngineGradlePluginFacetConfiguration getState(){
      return this;
    }

    @Override
    public void loadState(@NotNull AppEngineGradlePluginFacetConfiguration state) {
      this.gradleBuildDir = state.gradleBuildDir;
    }

    public String getGradleBuildDir() {
      return gradleBuildDir;
    }

    public void setGradleBuildDir(String gradleBuildDir) {
      this.gradleBuildDir = gradleBuildDir;
    }
  }
}
