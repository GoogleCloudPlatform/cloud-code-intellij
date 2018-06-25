/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.ModuleTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.facet.Facet;
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.FacetsProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineStandardFacetEditor}. */
public class AppEngineStandardFacetEditorTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  private AppEngineStandardFacetEditor standardFacetEditor;

  @Test
  public void getLibraryPanel_withNoBuildSystem_isVisible() {
    ModuleTestUtils.addFacet(module, AppEngineStandardFacetType.ID);

    standardFacetEditor = createEditor(module);

    assertThat(standardFacetEditor.getAppEngineStandardLibraryPanel().getLibraryPanel().isVisible())
        .isTrue();
  }

  private AppEngineStandardFacetEditor createEditor(Module module) {
    return new AppEngineStandardFacetEditor(
        new AppEngineStandardFacetConfiguration(), new TestFacetEditorContext(module));
  }

  private class TestFacetEditorContext implements FacetEditorContext {
    private Module module;

    TestFacetEditorContext(Module module) {
      this.module = module;
    }

    @NotNull
    @Override
    public Project getProject() {
      return module.getProject();
    }

    @Nullable
    @Override
    public Library findLibrary(@NotNull String name) {
      return null;
    }

    @Nullable
    @Override
    public ModuleBuilder getModuleBuilder() {
      return null;
    }

    @Override
    public boolean isNewFacet() {
      return false;
    }

    @NotNull
    @Override
    public Facet getFacet() {
      return AppEngineStandardFacet.getAppEngineFacetByModule(module);
    }

    @NotNull
    @Override
    public Module getModule() {
      return module;
    }

    @Nullable
    @Override
    public Facet getParentFacet() {
      return null;
    }

    @NotNull
    @Override
    public FacetsProvider getFacetsProvider() {
      return DefaultFacetsProvider.INSTANCE;
    }

    @NotNull
    @Override
    public ModulesProvider getModulesProvider() {
      return DefaultModulesProvider.EMPTY_MODULES_PROVIDER;
    }

    @NotNull
    @Override
    public ModifiableRootModel getModifiableRootModel() {
      return null;
    }

    @NotNull
    @Override
    public ModuleRootModel getRootModel() {
      return null;
    }

    @Override
    public Library[] getLibraries() {
      return new Library[0];
    }

    @Nullable
    @Override
    public WizardContext getWizardContext() {
      return null;
    }

    @Override
    public Library createProjectLibrary(String name, VirtualFile[] roots, VirtualFile[] sources) {
      return null;
    }

    @Override
    public VirtualFile[] getLibraryFiles(Library library, OrderRootType rootType) {
      return new VirtualFile[0];
    }

    @NotNull
    @Override
    public String getFacetName() {
      return AppEngineStandardFacetType.STRING_ID;
    }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
      return null;
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {}
  }
}
