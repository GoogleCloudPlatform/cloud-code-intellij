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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;

import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

/**
 * The Flexible facet configuration.
 *
 * <p>Stores the Flexible configuration files locations.
 */
public class AppEngineFlexibleFacetConfiguration implements FacetConfiguration,
    PersistentStateComponent<AppEngineFlexibleFacetConfiguration> {

  private String appYamlPath;
  private String dockerDirectory;

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext,
      FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[]{
      new FlexibleFacetEditor(this, editorContext.getModule())
    };
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    // Deprecated, do nothing.
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    // Deprecated, do nothing.
  }

  @Nullable
  @Override
  public AppEngineFlexibleFacetConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(AppEngineFlexibleFacetConfiguration state) {
    appYamlPath = state.getAppYamlPath();
    dockerDirectory = state.getDockerDirectory();
  }

  public String getAppYamlPath() {
    return appYamlPath;
  }

  public String getDockerDirectory() {
    return dockerDirectory;
  }

  public void setAppYamlPath(String appYamlPath) {
    this.appYamlPath = appYamlPath;
  }

  public void setDockerDirectory(String dockerDirectory) {
    this.dockerDirectory = dockerDirectory;
  }
}
