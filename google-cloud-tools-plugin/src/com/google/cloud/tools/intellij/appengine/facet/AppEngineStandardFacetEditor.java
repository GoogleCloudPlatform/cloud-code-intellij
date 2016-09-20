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

import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.collect.Sets;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * @author nik
 */
public class AppEngineStandardFacetEditor extends FacetEditorTab {

  private final AppEngineFacetConfiguration facetConfiguration;
  private final FacetEditorContext context;
  private JPanel myMainPanel;
  private AppEngineStandardLibraryPanel appEngineStandardLibraryPanel;

  public AppEngineStandardFacetEditor(AppEngineFacetConfiguration facetConfiguration,
      FacetEditorContext context) {
    this.facetConfiguration = facetConfiguration;
    this.context = context;
  }

  @Nls
  public String getDisplayName() {
    return "Google App Engine";
  }

  @NotNull
  public JComponent createComponent() {
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    Set<AppEngineStandardMavenLibrary> savedLibs = facetConfiguration.getLibraries();
    Set<AppEngineStandardMavenLibrary> selectedLibs
        = appEngineStandardLibraryPanel.getSelectedLibraries();

    return !Objects.equals(savedLibs, selectedLibs);
  }

  @Override
  public void apply() {
    Set<AppEngineStandardMavenLibrary> savedLibs = facetConfiguration.getLibraries();
    Set<AppEngineStandardMavenLibrary> selectedLibs
        = appEngineStandardLibraryPanel.getSelectedLibraries();

    // TODO need to also add / remove the library from the classpath
    Set<AppEngineStandardMavenLibrary> libsToAdd = Sets.difference(selectedLibs, savedLibs);
    Set<AppEngineStandardMavenLibrary> libsToRemove = Sets.difference(savedLibs, selectedLibs);

    facetConfiguration.setLibraries(selectedLibs);
  }

  @Override
  public void reset() {
    appEngineStandardLibraryPanel.setSelectedLibrary(facetConfiguration.getLibraries());
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  public void disposeUIResources() {
  }

  @Override
  public String getHelpTopic() {
    return "Google_App_Engine_Facet";
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    AppEngineWebIntegration.getInstance().setupDevServer();

    // Called on explicitly adding the facet through Project Settings -> Facets, but not on the
    // Framework discovered "Configure" popup.
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_ADD_FACET)
        .withLabel("setOnModule")
        .ping();
  }
}
