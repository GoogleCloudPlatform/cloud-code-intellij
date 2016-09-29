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
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExportableOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable.Listener;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.packaging.artifacts.Artifact;

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
  private JPanel mainPanel;
  private AppEngineStandardLibraryPanel appEngineStandardLibraryPanel;
  private Listener libraryListener;


  public AppEngineStandardFacetEditor(AppEngineFacetConfiguration facetConfiguration,
      FacetEditorContext context) {
    this.facetConfiguration = facetConfiguration;
    this.context = context;
    libraryListener = new LibraryModificationListener();

    LibraryTablesRegistrar.getInstance()
        .getLibraryTable(context.getProject()).addListener(libraryListener);
  }

  public String getDisplayName() {
    return "Google App Engine";
  }

  @NotNull
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    Set<AppEngineStandardMavenLibrary> savedLibs
        = facetConfiguration.getLibraries(context.getProject());
    Set<AppEngineStandardMavenLibrary> selectedLibs
        = appEngineStandardLibraryPanel.getSelectedLibraries();

    return !Objects.equals(savedLibs, selectedLibs);
  }

  @Override
  public void apply() {
    Set<AppEngineStandardMavenLibrary> savedLibs
        = facetConfiguration.getLibraries(context.getProject());
    Set<AppEngineStandardMavenLibrary> selectedLibs
        = appEngineStandardLibraryPanel.getSelectedLibraries();

    Set<AppEngineStandardMavenLibrary> libsToAdd = Sets.difference(selectedLibs, savedLibs);
    Set<AppEngineStandardMavenLibrary> libsToRemove = Sets.difference(savedLibs, selectedLibs);

    if (!libsToAdd.isEmpty()) {
      for (AppEngineStandardMavenLibrary library : libsToAdd) {
        AppEngineSupportProvider.loadMavenLibrary(context.getModule(), library);
      }
    }

    if (!libsToRemove.isEmpty()) {
      AppEngineSupportProvider.removeMavenLibraries(libsToRemove, context.getModule());
    }
  }

  @Override
  public void reset() {
    appEngineStandardLibraryPanel
        .setSelectLibraries(facetConfiguration.getLibraries(context.getProject()));
  }

  @SuppressWarnings("checkstyle:abbreviationaswordinname")
  public void disposeUIResources() {
    if (libraryListener != null) {
      LibraryTablesRegistrar.getInstance()
          .getLibraryTable(context.getProject()).removeListener(libraryListener);
    }
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

  public class LibraryModificationListener implements Listener {

    @Override
    public void afterLibraryAdded(final Library addedLibrary) {
      final DependencyScope scope = AppEngineStandardMavenLibrary
          .getLibraryByMavenDisplayName(addedLibrary.getName()).getScope();

      new WriteAction() {
        @Override
        protected void run(@NotNull Result result) throws Throwable {
          ModuleRootManager manager = ModuleRootManager.getInstance(context.getModule());
          ModifiableRootModel model = manager.getModifiableModel();

          model.addLibraryEntry(addedLibrary);

          for (OrderEntry orderEntry : model.getOrderEntries()) {
            if (orderEntry.getPresentableName().equals(addedLibrary.getName())) {
              ((ExportableOrderEntry) orderEntry).setScope(scope);
            }
          }
          model.commit();
        }
      }.execute();

      Artifact artifact = AppEngineSupportProvider
          .findOrCreateWebArtifact((AppEngineFacet) context.getFacet());
      AppEngineWebIntegration.getInstance()
          .addLibraryToArtifact(addedLibrary, artifact, context.getProject());

      appEngineStandardLibraryPanel.toggleLibrary(
          AppEngineStandardMavenLibrary.getLibraryByMavenDisplayName(addedLibrary.getName()),
          true /* select */);
    }

    @Override
    public void afterLibraryRemoved(Library removedLibrary) {
      ModuleRootManager manager = ModuleRootManager.getInstance(context.getModule());
      ModifiableRootModel model = manager.getModifiableModel();

      for (OrderEntry orderEntry : model.getOrderEntries()) {
        if (orderEntry.getPresentableName().equals(removedLibrary.getName())) {
          model.removeOrderEntry(orderEntry);
        }
      }
      model.commit();

      appEngineStandardLibraryPanel.toggleLibrary(
          AppEngineStandardMavenLibrary.getLibraryByMavenDisplayName(removedLibrary.getName()),
          false);
    }

    @Override
    public void afterLibraryRenamed(Library library) {
      // do nothing
    }

    @Override
    public void beforeLibraryRemoved(Library library) {
      // do nothing
    }
  }
}
