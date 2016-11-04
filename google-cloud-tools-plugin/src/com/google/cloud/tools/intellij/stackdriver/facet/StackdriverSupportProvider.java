/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.facet;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;

import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Adds Google Stackdriver support to IntelliJ IDEA Java projects.
 */
public class StackdriverSupportProvider extends FrameworkSupportInModuleProvider {

  @NotNull
  @Override
  public FrameworkTypeEx getFrameworkType() {
    return StackdriverFrameworkType.getFrameworkType();
  }

  @NotNull
  @Override
  public FrameworkSupportInModuleConfigurable createConfigurable(
      @NotNull FrameworkSupportModel model) {
    return new StackdriverSupportConfigurable();
  }

  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module,
      @NotNull FacetsProvider facetsProvider) {
    return !facetsProvider.getFacetsByType(module, StackdriverFacetType.ID).isEmpty();
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  private static class StackdriverSupportConfigurable extends FrameworkSupportInModuleConfigurable {

    private StackdriverPanel stackdriverPanel;

    public StackdriverSupportConfigurable() {
      stackdriverPanel = new StackdriverPanel(new StackdriverFacetConfiguration(), true);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
      return stackdriverPanel.createComponent();
    }

    @Override
    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel,
        @NotNull ModifiableModelsProvider modifiableModelsProvider) {
      FacetType<StackdriverFacet, StackdriverFacetConfiguration> facetType =
          new StackdriverFacetType();
      StackdriverFacetConfiguration configuration = FacetManager.getInstance(module)
          .addFacet(facetType, facetType.getPresentableName(), null /* underlying */)
          .getConfiguration();
      configuration.getState().setGenerateSourceContext(
          stackdriverPanel.isGenerateSourceContextSelected());
      configuration.getState().setIgnoreErrors(stackdriverPanel.isIgnoreErrorsSelected());
      configuration.getState().setCloudSdkPath(
          CloudSdkService.getInstance().getSdkHomePath().toString());
      configuration.getState().setModuleSourceDirectory(
          stackdriverPanel.getModuleSourceDirectory());
    }
  }
}
