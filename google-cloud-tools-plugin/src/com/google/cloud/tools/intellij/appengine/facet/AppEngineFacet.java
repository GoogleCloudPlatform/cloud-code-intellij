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

import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdk;
import com.google.cloud.tools.intellij.appengine.sdk.AppEngineSdkManager;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class AppEngineFacet extends Facet<AppEngineFacetConfiguration> {

  public static final FacetTypeId<AppEngineFacet> ID = new FacetTypeId<AppEngineFacet>("appEngine");

  public AppEngineFacet(@NotNull FacetType facetType,
      @NotNull Module module,
      @NotNull String name,
      @NotNull AppEngineFacetConfiguration configuration) {
    super(facetType, module, name, configuration, null);
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

  @NotNull
  public AppEngineSdk getSdk() {
    return AppEngineSdkManager.getInstance().findSdk(getConfiguration().getSdkHomePath());
  }

  //todo[nik] copied from JamCommonUtil
  @Nullable
  private static <T> T getRootElement(final PsiFile file, final Class<T> domClass,
      final Module module) {
    if (!(file instanceof XmlFile)) {
      return null;
    }
    final DomManager domManager = DomManager.getDomManager(file.getProject());
    final DomFileElement<DomElement> element = domManager
        .getFileElement((XmlFile) file, DomElement.class);
    if (element == null) {
      return null;
    }
    final DomElement root = element.getRootElement();
    if (!ReflectionUtil.isAssignable(domClass, root.getClass())) {
      return null;
    }
    return (T) root;
  }


  public boolean shouldRunEnhancerFor(@NotNull VirtualFile file) {
    for (String path : getConfiguration().getFilesToEnhance()) {
      final VirtualFile toEnhance = LocalFileSystem.getInstance().findFileByPath(path);
      if (toEnhance != null && VfsUtilCore.isAncestor(toEnhance, file, false)) {
        return true;
      }
    }
    return false;
  }
}
