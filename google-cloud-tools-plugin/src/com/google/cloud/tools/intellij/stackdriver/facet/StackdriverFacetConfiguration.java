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

import com.google.cloud.tools.intellij.stackdriver.facet.StackdriverFacetConfiguration.StackdriverProperties;

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
 * Stores the facet configuration and handles its persistence.
 */
public class StackdriverFacetConfiguration
    implements FacetConfiguration, PersistentStateComponent<StackdriverProperties> {

  private StackdriverProperties persistedProperties = new StackdriverProperties();

  @Override
  public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext,
      FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[]{
        new StackdriverPanel(editorContext)
    };
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    // Deprecated.
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    // Deprecated.
  }

  @Nullable
  @Override
  public StackdriverProperties getState() {
    return persistedProperties;
  }

  @Override
  public void loadState(StackdriverProperties state) {
    persistedProperties = state;
  }

  public static class StackdriverProperties {
    private boolean generateSourceContext = true;
    private boolean ignoreErrors = true;

    public boolean isGenerateSourceContext() {
      return generateSourceContext;
    }

    public void setGenerateSourceContext(boolean generateSourceContext) {
      this.generateSourceContext = generateSourceContext;
    }

    public boolean isIgnoreErrors() {
      return ignoreErrors;
    }

    public void setIgnoreErrors(boolean ignoreErrors) {
      this.ignoreErrors = ignoreErrors;
    }
  }
}
