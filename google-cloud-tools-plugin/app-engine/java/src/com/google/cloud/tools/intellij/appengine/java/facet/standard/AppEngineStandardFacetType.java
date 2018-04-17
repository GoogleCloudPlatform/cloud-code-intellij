/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** @author nik */
public class AppEngineStandardFacetType
    extends FacetType<AppEngineStandardFacet, AppEngineStandardFacetConfiguration> {

  public static final FacetTypeId<AppEngineStandardFacet> ID = new FacetTypeId<>("appEngine");
  public static final String STRING_ID = "app-engine-standard";

  public AppEngineStandardFacetType() {
    super(ID, STRING_ID, AppEngineMessageBundle.message("appengine.standard.facet.name.title"));
  }

  @Override
  public AppEngineStandardFacetConfiguration createDefaultConfiguration() {
    return new AppEngineStandardFacetConfiguration();
  }

  @Override
  public AppEngineStandardFacet createFacet(
      @NotNull Module module,
      String name,
      @NotNull AppEngineStandardFacetConfiguration configuration,
      @Nullable Facet underlyingFacet) {
    return new AppEngineStandardFacet(this, module, name, configuration);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @Override
  public Icon getIcon() {
    return AppEngineIcons.APP_ENGINE;
  }
}
