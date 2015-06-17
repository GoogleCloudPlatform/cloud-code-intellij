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
 * A configuration for App Engine Gradle Facets that is populated during gradle project import
 */
public class AppEngineGradleFacetConfiguration implements FacetConfiguration, PersistentStateComponent<AppEngineConfigurationProperties> {
  AppEngineConfigurationProperties myProperties = new AppEngineConfigurationProperties();

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[] {
       // Add editor tab here for App Engine Gradle modules
    };
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    //Deprecated abstract method, using persistent state component now
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    //Deprecated abstract method, using persistent state component now
  }

  @Nullable
  @Override
  public AppEngineConfigurationProperties getState() {
    return myProperties;
  }

  @Override
  public void loadState(AppEngineConfigurationProperties state) {
    myProperties = state;
  }
}
