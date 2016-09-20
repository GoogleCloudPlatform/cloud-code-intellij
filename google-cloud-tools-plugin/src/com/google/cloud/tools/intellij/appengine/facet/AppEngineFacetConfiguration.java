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

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacetConfiguration.AppEngineFacetProperties;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.annotations.Tag;

import org.jdom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * @author nik
 */
public class AppEngineFacetConfiguration implements FacetConfiguration,
    PersistentStateComponent<AppEngineFacetProperties> {

  private AppEngineFacetProperties properties
      = new AppEngineFacetProperties();

  public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext,
      FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[]{
        new AppEngineStandardFacetEditor(this, editorContext)
    };
  }

  public void readExternal(Element element) throws InvalidDataException {
  }

  public void writeExternal(Element element) throws WriteExternalException {
  }

  public Set<AppEngineStandardMavenLibrary> getLibraries() {
    return properties.libraries;
  }

  public void setLibraries(Set<AppEngineStandardMavenLibrary> libraries) {
    properties.libraries = libraries;
  }

  @Override
  public AppEngineFacetProperties getState() {
    return properties;
  }

  @Override
  public void loadState(AppEngineFacetProperties state) {
    properties = state;
  }

  public static class AppEngineFacetProperties {

    @Tag("libraries")
    public Set<AppEngineStandardMavenLibrary> libraries = new HashSet<>();
  }
}
