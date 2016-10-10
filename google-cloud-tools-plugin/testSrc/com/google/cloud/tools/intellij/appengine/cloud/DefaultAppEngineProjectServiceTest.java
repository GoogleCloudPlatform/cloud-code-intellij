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

import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.DefaultAppEngineProjectService;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestCase;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Unit tests for {@link DefaultAppEngineProjectService}
 */
public class DefaultAppEngineProjectServiceTest extends PlatformTestCase {

  private AppEngineProjectService appEngineProjectService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Fixes https://youtrack.jetbrains.com/issue/IDEA-129297. Only occurs in Jenkins.
    VfsRootAccess.allowRootAccess(System.getProperty("user.dir"));
    appEngineProjectService = new DefaultAppEngineProjectService();
  }

  public void testGetAppEngineArtifactEnvironment_Standard() {
    addAppEngineFacet(createModule("myModule"));

    XmlFile flexCompatWebXml = loadTestWebXml("testData/descriptor/appengine-web.xml");

    AppEngineEnvironment environment
        = appEngineProjectService.getModuleAppEngineEnvironment(flexCompatWebXml);

    assertEquals(AppEngineEnvironment.APP_ENGINE_STANDARD, environment);
  }

  public void testGetAppEngineArtifactEnvironment_Flexible() {
    addAppEngineFacet(createModule("myModule"));

    // JAR Artifact Type
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(null /*appengine-web.xml*/));

    // WAR Artifact Type
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(null /*appengine-web.xml*/));
  }

  public void testGetAppEngineArtifactEnvironment_FlexibleCompat() {
    addAppEngineFacet(createModule("myModule"));

    // Load flex-compat appengine-web.xml with vm: true
    XmlFile vmTrueWebXml = loadTestWebXml("testData/descriptor/appengine-web_flex-compat_vm.xml");
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(vmTrueWebXml));

    // Load flex-compat appengine-web.xml with env: flex
    XmlFile envFlexWebXml = loadTestWebXml("testData/descriptor/appengine-web_flex-compat_env.xml");
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(envFlexWebXml));
  }

  public void testGetAppEngineStandardDeclaredRuntime_NullArg() {
    assertNull(appEngineProjectService.getAppEngineStandardDeclaredRuntime(null));
  }

  public void testGetAppEngineStandardDeclaredRuntime_NoneDeclared() {
    XmlFile appEngineWebXml = loadTestWebXml("testData/descriptor/appengine-web.xml");
    assertNull(appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
  }

  public void testGetAppEngineStandardDeclaredRuntime_Java8() {
    XmlFile appEngineWebXml = loadTestWebXml("testData/descriptor/appengine-web_runtime-java8.xml");
    assertEquals(AppEngineStandardRuntime.JAVA_8,
        appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
  }

  public void testGetAppEngineStandardDeclaredRuntime_Invalid() {
    XmlFile appEngineWebXml = loadTestWebXml(
        "testData/descriptor/appengine-web_runtime-invalid.xml");
    assertNull(appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
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

  private XmlFile loadTestWebXml(String path) {
    VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(
        new File(path));

    return vFile == null
        ? null
        : (XmlFile) PsiManager.getInstance(getProject()).findFile(vFile);
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
}
