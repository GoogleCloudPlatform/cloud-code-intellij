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

package com.google.cloud.tools.intellij.appengine.cloud;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProvider;
import com.google.cloud.tools.intellij.appengine.project.AppEngineAssetProviderImpl;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectServiceImpl;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.impl.artifacts.ArtifactImpl;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.packaging.impl.elements.ArtifactRootElementImpl;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElementBase;
import com.intellij.packaging.impl.elements.TestModuleOutputPackagingElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestCase;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

import java.io.File;

import javax.swing.Icon;

/**
 * Unit tests for {@link AppEngineProjectServiceImpl}
 */
public class AppEngineProjectServiceImplTest extends PlatformTestCase {

  private AppEngineProjectService appEngineProjectService;
  private AppEngineAssetProvider appEngineAssetProvider;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();

    appEngineAssetProvider = mock(AppEngineAssetProviderImpl.class);
    applicationContainer.unregisterComponent(AppEngineAssetProvider.class.getName());
    applicationContainer.registerComponentInstance(
        AppEngineAssetProvider.class.getName(), appEngineAssetProvider);

    appEngineProjectService = new AppEngineProjectServiceImpl();
  }

  public void testGetAppEngineArtifactEnvironment_Standard() {
    Artifact artifact = createTestArtifact(new ExplodedWarArtifactTestType());
    addAppEngineFacet(createModule("myModule"));
    AppEngineEnvironment environment
        = appEngineProjectService.getAppEngineArtifactEnvironment(getProject(), artifact);

    assertEquals(AppEngineEnvironment.APP_ENGINE_STANDARD, environment);
  }

  public void testGetAppEngineArtifactEnvironment_Flexible() {
    addAppEngineFacet(createModule("myModule"));

    // JAR Artifact Type
    Artifact jarArtifact = createTestArtifact(new JarArtifactType());
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getAppEngineArtifactEnvironment(getProject(), jarArtifact));

    // WAR Artifact Type
    Artifact warArtifact = createTestArtifact(new JarArtifactType());
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getAppEngineArtifactEnvironment(getProject(), warArtifact));
  }

  public void testGetAppEngineArtifactEnvironment_FlexibleCompat() {
    Artifact artifact = createTestArtifact(new ExplodedWarArtifactTestType());
    addAppEngineFacet(createModule("myModule"));

    // Mock the flex-compat appengine-web.xml
    when(appEngineAssetProvider
        .loadAppEngineStandardWebXml(any(Project.class), any(Artifact.class)))
        .thenReturn(loadTestFlexCompatWebXml());

    AppEngineEnvironment environment
        = appEngineProjectService.getAppEngineArtifactEnvironment(getProject(), artifact);
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX, environment);
  }

  public void testIsAppEngineStandardArtifact() {
    Artifact artifact = createTestArtifact(new ExplodedWarArtifactTestType());
    addAppEngineFacet(createModule("myModule"));

    assertTrue(appEngineProjectService.isAppEngineStandardArtifact(getProject(), artifact));
  }

  public void testHasAppEngineStandardFacet_FalseExpected() {
    Artifact artifact = createTestArtifact(new ExplodedWarArtifactTestType());

    assertFalse(appEngineProjectService.hasAppEngineStandardFacet(getProject(), artifact));
  }

  public void testHasAppEngineArtifact_TrueExpected() {
    Artifact artifact = createTestArtifact(new ExplodedWarArtifactTestType());
    addAppEngineFacet(createModule("myModule"));

    assertTrue(appEngineProjectService.hasAppEngineStandardFacet(getProject(), artifact));
  }

  private void addAppEngineFacet(final Module module) {
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        FacetManager.getInstance(module).addFacet(
            new AppEngineFacetTestType(), "Google App Engine", null);
      }
    }.execute();
  }

  private XmlFile loadTestFlexCompatWebXml() {
    VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(
        new File("testData/descriptor/appengine-web_flex-compat.xml"));

    return vFile == null
        ? null
        : (XmlFile) PsiManager.getInstance(getProject()).findFile(vFile);
  }

  @SuppressWarnings("unchecked")
  private Artifact createTestArtifact(ArtifactType artifactType) {
    CompositePackagingElement compositePackage = new ArtifactRootElementImpl();
    ModulePointer modulePointer =
        ModulePointerManager.getInstance(getProject()).create("myModule");
    ModuleOutputPackagingElementBase modulePackagingElement
        = new TestModuleOutputPackagingElement(getProject(), modulePointer);
    compositePackage.addFirstChild(modulePackagingElement);

    return new ArtifactImpl("myArtifact", artifactType, true, compositePackage, "/a/b/c");
  }

  @SuppressWarnings("unchecked")
  private static class AppEngineFacetTestType extends FacetType {
    AppEngineFacetTestType() {
      super(new FacetTypeId("id"), "id", "facetType");
    }

    @Override
    public FacetConfiguration createDefaultConfiguration() {
      return createFacetConfiguration();
    }

    @Override
    public Facet createFacet(@NotNull Module module, String name,
        @NotNull FacetConfiguration configuration, @Nullable Facet underlyingFacet) {
      return new Facet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
      return false;
    }

    private FacetConfiguration createFacetConfiguration() {
      return new FacetConfiguration() {
        @Override
        public FacetEditorTab[] createEditorTabs(FacetEditorContext editorContext,
            FacetValidatorsManager validatorsManager) {
          return new FacetEditorTab[0];
        }

        @Override
        public void readExternal(Element element) throws InvalidDataException {

        }

        @Override
        public void writeExternal(Element element) throws WriteExternalException {

        }
      };
    }
  }

  private static class ExplodedWarArtifactTestType extends ArtifactType {

    public ExplodedWarArtifactTestType() {
      super("exploded-war", "exploded-war");
    }

    @NotNull
    @Override
    public Icon getIcon() {
      return null;
    }

    @Nullable
    @Override
    public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
      return null;
    }

    @NotNull
    @Override
    public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
      return new ArtifactRootElementImpl();
    }
  }
}
