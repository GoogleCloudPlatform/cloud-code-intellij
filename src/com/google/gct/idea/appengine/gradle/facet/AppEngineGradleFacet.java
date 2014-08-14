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

import com.google.common.base.Strings;
import com.google.gct.idea.appengine.dom.AppEngineWebApp;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * App Engine Gradle facet for App Engine Modules with a Gradle build file
 */
public class AppEngineGradleFacet extends Facet<AppEngineGradleFacetConfiguration> {
  private static final Logger LOG = Logger.getInstance(AppEngineGradleFacet.class);

  @NonNls public static final String ID = "app-engine-gradle";
  @NonNls public static final String NAME = "App Engine Gradle";

  public static final FacetTypeId<AppEngineGradleFacet> TYPE_ID = new FacetTypeId<AppEngineGradleFacet>(ID);

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

  /**
   * Returns an object holding information from the appengine-web.xml file.
   */
  public AppEngineWebApp getAppEngineWebXml() {
    AppEngineConfigurationProperties model = getConfiguration().getState();
    if (model == null || Strings.isNullOrEmpty(model.WEB_APP_DIR)) {
      return null;
    }

    String path = model.WEB_APP_DIR + "/WEB-INF/appengine-web.xml";
    VirtualFile appEngineFile = LocalFileSystem.getInstance().findFileByPath(path.replace(File.separatorChar, '/'));
    if (appEngineFile == null) {
      return null;
    }

    PsiFile psiFile = PsiManager.getInstance(getModule().getProject()).findFile(appEngineFile);
    if (psiFile == null || !(psiFile instanceof XmlFile)) {
      return null;
    }

    final DomManager domManager = DomManager.getDomManager(getModule().getProject());
    return domManager.getFileElement((XmlFile)psiFile, AppEngineWebApp.class).getRootElement();
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
