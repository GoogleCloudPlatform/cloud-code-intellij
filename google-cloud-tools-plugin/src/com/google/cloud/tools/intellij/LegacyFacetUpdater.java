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

package com.google.cloud.tools.intellij;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacetType;
import com.google.cloud.tools.intellij.util.Plugins;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.impl.FacetUtil;
import com.intellij.facet.impl.invalid.InvalidFacet;
import com.intellij.facet.impl.invalid.InvalidFacetConfiguration;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Created by alexsloan on 7/27/16.
 */
public class LegacyFacetUpdater implements ProjectComponent {

  private final static String DEPRECATED_APP_ENGINE_FACET_ID = "google-app-engine";
  private final static String DEPRECATED_PLUGIN_ID = "com.intellij.appengine";
  private final static String COMPONENT_NAME = "Legacy Facet Updater";

  private Project project;

  public LegacyFacetUpdater(Project project) {
    this.project = project;
  }

  @Override
  public void projectOpened() {
    // TODO: is there a better way to identify this plugin than just this plugin ID? is it guaranteed that another matching one can't be created later?
    //  - could mitigate by checking isBundled for the plugin?
    if (! Plugins.isPluginInstalled(DEPRECATED_PLUGIN_ID)) {
     replaceReferencesToOldFacet();
    }
  }

  private void replaceReferencesToOldFacet() {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    for (Module module : modules) {
      Facet[] existingFacets = FacetManager.getInstance(module).getAllFacets();
      // see also ProjectFacetManager.getFacets -- (gets facets by facettypeid)
      for (Facet facet : existingFacets) {
        if (isDeprecatedAppEngineFacet(facet)){
          // TODO: use some combination of FacetManager and ProjectFacetManager to:
          // 1) force the facet replacement to be persisted
          // 2) copy the configuration from the first facet to the second
          FacetUtil.deleteFacet(facet);
          FacetUtil.addFacet(module, FacetType.findInstance(AppEngineFacetType.class));
        }
      }
    }
  }

  private boolean isDeprecatedAppEngineFacet(Facet facet) {
    if (facet == null || facet.getType() == null) {
      return false;
    }

    if (! facet.getClass().isAssignableFrom((InvalidFacet.class))) {
      return false;
    }

    // TODO is this a reliable way of determining this??
    InvalidFacetConfiguration configuration = ((InvalidFacet) facet).getConfiguration();
    String facetId = configuration.getFacetState().getFacetType();
    return facetId.equals(DEPRECATED_APP_ENGINE_FACET_ID);
  }

  @Override
  public void projectClosed() {
    // Do nothing.
  }

  @Override
  public void initComponent() {
    // Do nothing.
  }

  @Override
  public void disposeComponent() {
    // Do nothing.
  }

  @NotNull
  @Override
  public String getComponentName() {
    return COMPONENT_NAME;
  }
}
