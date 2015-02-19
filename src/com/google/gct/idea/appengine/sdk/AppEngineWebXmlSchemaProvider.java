/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.appengine.sdk;

import com.google.gct.idea.appengine.gradle.facet.AppEngineConfigurationProperties;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Provider to connect appengine-web.xml namespaces with the correct file in the SDK
 */
public class AppEngineWebXmlSchemaProvider extends XmlSchemaProvider {

  @Override
  public boolean isAvailable(@NotNull XmlFile file) {
    if (!"appengine-web.xml".equals(file.getName())) {
      return false;
    }
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
    if (facet != null) {
      AppEngineConfigurationProperties state = facet.getConfiguration().getState();
      return (state != null && StringUtil.isNotEmpty(state.APPENGINE_SDKROOT));
    }
    return false;
  }

  @Nullable
  @Override
  public XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
    if (module == null) {
      return null;
    }

    if (!url.equals("http://appengine.google.com/ns/1.0")) {
      return null;
    }

    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(module);
    if (facet != null) {
      AppEngineSdk sdk = facet.getAppEngineSdk();
      if (sdk != null) {
        File file = sdk.getXmlDescriptorFile();
        if (file != null) {
          VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
          if (virtualFile != null) {
            PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(virtualFile);
            if (psiFile instanceof XmlFile) {
              return (XmlFile)psiFile;
            }
          }
        }
      }
    }
    return null;
  }

}
