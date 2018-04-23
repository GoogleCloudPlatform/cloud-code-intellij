/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import static java.util.stream.Collectors.toSet;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacetConfiguration.AppEngineStandardFacetProperties;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/** @author nik */
public class AppEngineStandardFacetConfiguration
    implements FacetConfiguration, PersistentStateComponent<AppEngineStandardFacetProperties> {

  private AppEngineStandardFacetProperties properties = new AppEngineStandardFacetProperties();

  @Override
  public FacetEditorTab[] createEditorTabs(
      FacetEditorContext editorContext, FacetValidatorsManager validatorsManager) {
    return new FacetEditorTab[] {new AppEngineStandardFacetEditor(this, editorContext)};
  }

  public void readExternal(Element element) throws InvalidDataException {}

  public void writeExternal(Element element) throws WriteExternalException {}

  /**
   * Looks up the user's configured libraries for the project and returns a set of {@link
   * AppEngineStandardMavenLibrary} for each configured library matching one of the AE standard
   * managed libraries.
   *
   * <p>The lookup is performed based on the maven display id consisting of groupId, artifactName,
   * and version. If the configured lib doesn't entirely match this strategy, then it will not be
   * returned and therefore not considered to be "managed".
   */
  public Set<AppEngineStandardMavenLibrary> getLibraries(@NotNull Project project) {
    return Arrays.stream(
            LibraryTablesRegistrar.getInstance().getLibraryTable(project).getLibraries())
        .map(
            library ->
                AppEngineStandardMavenLibrary.getLibraryByMavenDisplayName(library.getName()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());
  }

  @Override
  public AppEngineStandardFacetProperties getState() {
    return properties;
  }

  @Override
  public void loadState(AppEngineStandardFacetProperties state) {
    properties = state;
  }

  public static class AppEngineStandardFacetProperties {}
}
