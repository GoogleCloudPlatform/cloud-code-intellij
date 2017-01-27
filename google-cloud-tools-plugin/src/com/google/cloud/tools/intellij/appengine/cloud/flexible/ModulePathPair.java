/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.ui.ListCellRendererWrapper;

import javax.swing.JList;

/**
 * IntelliJ module - file path tuple to populate file combo boxes in
 * {@link AppEngineFlexibleDeploymentEditor}.
 */
public class ModulePathPair {
  private Module module;
  private String path;

  public ModulePathPair(Module module, ConfigurationFileType type) {
    this.module = module;

    AppEngineFlexibleFacet flexFacet =
        FacetManager.getInstance(module).getFacetByType(AppEngineFlexibleFacetType.ID);
    if (flexFacet != null) {
      this.path = type.equals(ConfigurationFileType.APP_YAML)
          ? flexFacet.getConfiguration().getAppYamlPath()
          : flexFacet.getConfiguration().getDockerfilePath();
    }
  }

  public Module getModule() {
    return module;
  }

  public String getPath() {
    return path;
  }

  public enum ConfigurationFileType {
    APP_YAML,
    DOCKERFILE
  }

  public static class ModulePathPairRenderer extends ListCellRendererWrapper<ModulePathPair> {
    @Override
    public void customize(JList list, ModulePathPair value, int index, boolean selected,
        boolean hasFocus) {
      if (value != null) {
        setText(value.getModule().getName());
      }
    }
  }
}
