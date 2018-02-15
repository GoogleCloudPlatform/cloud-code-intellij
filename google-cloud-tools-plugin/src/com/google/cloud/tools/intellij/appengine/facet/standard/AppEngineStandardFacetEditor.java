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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.collect.Sets;
import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ExportableOrderEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable.Listener;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.packaging.artifacts.Artifact;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/** @author nik */
public class AppEngineStandardFacetEditor extends FacetEditorTab {

  private final AppEngineStandardFacetConfiguration facetConfiguration;
  private final FacetEditorContext context;
  private JPanel mainPanel;
  private AppEngineStandardLibraryPanel appEngineStandardLibraryPanel;
  private Listener libraryListener;

  public AppEngineStandardFacetEditor(
      AppEngineStandardFacetConfiguration facetConfiguration, FacetEditorContext context) {
    this.facetConfiguration = facetConfiguration;
    this.context = context;
    libraryListener = new LibraryModificationListener();

    LibraryTablesRegistrar.getInstance()
        .getLibraryTable(context.getProject())
        .addListener(libraryListener);
  }

  @Override
  public String getDisplayName() {
    return GctBundle.message("appengine.standard.facet.name.title");
  }

  @NotNull
  @Override
  public JComponent createComponent() {
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    if (!appEngineStandardLibraryPanel.isEnabled()) {
      return false;
    }

    Set<AppEngineStandardMavenLibrary> savedLibs =
        facetConfiguration.getLibraries(context.getProject());
    Set<AppEngineStandardMavenLibrary> selectedLibs =
        appEngineStandardLibraryPanel.getSelectedLibraries();

    return !Objects.equals(savedLibs, selectedLibs);
  }

  @Override
  public void apply() {
    if (appEngineStandardLibraryPanel.isEnabled()) {
      Set<AppEngineStandardMavenLibrary> savedLibs =
          facetConfiguration.getLibraries(context.getProject());
      Set<AppEngineStandardMavenLibrary> selectedLibs =
          appEngineStandardLibraryPanel.getSelectedLibraries();

      Set<AppEngineStandardMavenLibrary> libsToAdd = Sets.difference(selectedLibs, savedLibs);
      Set<AppEngineStandardMavenLibrary> libsToRemove = Sets.difference(savedLibs, selectedLibs);

      if (!libsToAdd.isEmpty()) {
        for (AppEngineStandardMavenLibrary library : libsToAdd) {
          MavenRepositoryLibraryDownloader.getInstance()
              .downloadLibrary(context.getModule(), library);
        }
      }

      if (!libsToRemove.isEmpty()) {
        AppEngineStandardSupportProvider.removeMavenLibraries(libsToRemove, context.getModule());
      }
    }
  }

  @Override
  public void reset() {
    if (appEngineStandardLibraryPanel.isEnabled()) {
      appEngineStandardLibraryPanel.setSelectedLibraries(
          facetConfiguration.getLibraries(context.getProject()));
    }
  }

  @Override
  public void disposeUIResources() {
    if (libraryListener != null) {
      LibraryTablesRegistrar.getInstance()
          .getLibraryTable(context.getProject())
          .removeListener(libraryListener);
    }
  }

  @Override
  public String getHelpTopic() {
    return "Google_App_Engine_Facet";
  }

  @Override
  public void onFacetInitialized(@NotNull Facet facet) {
    AppEngineStandardWebIntegration.getInstance().setupDevServer();

    // Called on explicitly adding the facet through Project Settings -> Facets, but not on the
    // Framework discovered "Configure" popup.
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_FACET_ADD)
        .addMetadata("source", "setOnModule")
        .addMetadata("env", "standard")
        .ping();
  }

  private void createUIComponents() {
    appEngineStandardLibraryPanel = new AppEngineStandardLibraryPanel(isManagedLibrariesEnabled());
  }

  /**
   * Currently, managed AE standard library support is enabled only for native IJ projects and
   * explicitly disabled for Maven / Gradle projects to avoid dependency conflicts.
   */
  private boolean isManagedLibrariesEnabled() {
    AppEngineProjectService projectService = AppEngineProjectService.getInstance();
    return !projectService.isMavenModule(context.getModule())
        && !projectService.isGradleModule(context.getModule());
  }

  public class LibraryModificationListener implements Listener {

    @Override
    public void afterLibraryAdded(final Library addedLibrary) {
      Optional<AppEngineStandardMavenLibrary> libraryOptional =
          AppEngineStandardMavenLibrary.getLibraryByMavenDisplayName(addedLibrary.getName());

      libraryOptional.ifPresent(
          library -> {
            final DependencyScope scope = library.getScope();

            ApplicationManager.getApplication()
                .runWriteAction(
                    () -> {
                      ModifiableRootModel model =
                          ModuleRootManager.getInstance(context.getModule()).getModifiableModel();

                      model.addLibraryEntry(addedLibrary);

                      Arrays.stream(model.getOrderEntries())
                          .filter(
                              orderEntry ->
                                  orderEntry.getPresentableName().equals(addedLibrary.getName()))
                          .forEach(
                              orderEntry -> ((ExportableOrderEntry) orderEntry).setScope(scope));

                      model.commit();
                    });

            Artifact artifact =
                AppEngineStandardSupportProvider.findOrCreateWebArtifact(
                    (AppEngineStandardFacet) context.getFacet());
            AppEngineStandardWebIntegration.getInstance()
                .addLibraryToArtifact(addedLibrary, artifact, context.getProject());

            appEngineStandardLibraryPanel.toggleLibrary(library, true /* select */);
          });
    }

    @Override
    public void afterLibraryRemoved(Library removedLibrary) {
      AppEngineStandardMavenLibrary.getLibraryByMavenDisplayName(removedLibrary.getName())
          .ifPresent(
              library -> appEngineStandardLibraryPanel.toggleLibrary(library, false /* select */));
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
