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

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.IdeaModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.utils.library.RepositoryAddLibraryAction;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibrarySupport;
import org.jetbrains.idea.maven.utils.library.propertiesEditor.RepositoryLibraryPropertiesDialog;
import org.jetbrains.idea.maven.utils.library.propertiesEditor.RepositoryLibraryPropertiesModel;

/**
 * Extends {@link RepositoryAddLibraryAction} to allow setting a custom library version in the
 * action.
 */
public class RepositoryWithVersionAddLibraryAction extends RepositoryAddLibraryAction {

  private final String version;
  private final Module module;
  private final RepositoryLibraryDescription libraryDescription;

  public RepositoryWithVersionAddLibraryAction(
      Module module,
      @NotNull RepositoryLibraryDescription libraryDescription,
      String version) {
    super(module, libraryDescription);
    this.module = module;
    this.libraryDescription = libraryDescription;
    this.version = version;
  }

  /**
   * Similar to {@link RepositoryAddLibraryAction#applyFix(Project, CommonProblemDescriptor)}
   * except that this sets the passed in version string instead of hardcoding it to "RELEASE" as is
   * done in the overridden method.
   */
  @Override
  public void applyFix(@NotNull Project project, PsiFile file, @Nullable Editor editor) {
    RepositoryLibraryPropertiesModel model = new RepositoryLibraryPropertiesModel(version, false,
        false);
    RepositoryLibraryPropertiesDialog dialog = new RepositoryLibraryPropertiesDialog(project, model,
        this.libraryDescription, false);
    if (dialog.showAndGet()) {
      IdeaModifiableModelsProvider modifiableModelsProvider = new IdeaModifiableModelsProvider();
      final ModifiableRootModel modifiableModel
          = ModuleRootManager.getInstance(module).getModifiableModel();
      RepositoryLibrarySupport librarySupport = new RepositoryLibrarySupport(project,
          this.libraryDescription, model);

      librarySupport.addSupport(this.module, modifiableModel, modifiableModelsProvider);
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        public void run() {
          modifiableModel.commit();
        }
      });
    }
  }
}
