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

package com.google.cloud.tools.intellij.appengine.facet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.IdeaModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;
import org.jetbrains.idea.maven.utils.library.RepositoryLibrarySupport;
import org.jetbrains.idea.maven.utils.library.propertiesEditor.RepositoryLibraryPropertiesModel;

/**
 * Downloads a maven library and adds it to a module.
 */
public class MavenRepositoryLibraryDownloader {

  public static MavenRepositoryLibraryDownloader getInstance() {
    return ServiceManager.getService(MavenRepositoryLibraryDownloader.class);
  }

  @Nullable
  public Library downloadLibrary(Module module, AppEngineStandardMavenLibrary library) {
    RepositoryLibraryProperties libraryProperties = new RepositoryLibraryProperties(
        library.getGroupId(), library.getArtifactId(), library.getVersion());
    RepositoryLibraryDescription libraryDescription =
        RepositoryLibraryDescription.findDescription(libraryProperties);
    RepositoryLibraryPropertiesModel model = new RepositoryLibraryPropertiesModel(
        library.getVersion(),
        true /*downloadSources*/,
        true /*downloadJavaDocs*/);

    IdeaModifiableModelsProvider modifiableModelsProvider = new IdeaModifiableModelsProvider();
    final ModifiableRootModel modifiableModel
        = ModuleRootManager.getInstance(module).getModifiableModel();
    RepositoryLibrarySupport librarySupport = new RepositoryLibrarySupport(module.getProject(),
        libraryDescription, model);

    librarySupport.addSupport(module, modifiableModel, modifiableModelsProvider);
    ApplicationManager.getApplication().runWriteAction(modifiableModel::commit);

    LibraryTable.ModifiableModel libraryTableModifiableModel
        = ModifiableModelsProvider.SERVICE.getInstance()
          .getLibraryTableModifiableModel(module.getProject());

    return libraryTableModifiableModel.getLibraryByName(library.toMavenDisplayVersion());
 }
}
