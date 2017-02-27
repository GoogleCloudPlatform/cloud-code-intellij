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

package com.google.cloud.tools.intellij.appengine.project;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardRuntime;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;

import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestCase;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Unit tests for {@link DefaultAppEngineProjectService}
 */
public class DefaultAppEngineProjectServiceTest extends PlatformTestCase {

  private DefaultAppEngineProjectService appEngineProjectService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Fixes https://youtrack.jetbrains.com/issue/IDEA-129297. Only occurs in Jenkins.
    VfsRootAccess.allowRootAccess(System.getProperty("user.dir"));
    appEngineProjectService = new DefaultAppEngineProjectService();
  }

  public void testGetAppEngineArtifactEnvironment_Standard() {
    Module module = createModule("myModule");
    addAppEngineStandardFacet(module);

    assertEquals(AppEngineEnvironment.APP_ENGINE_STANDARD,
        appEngineProjectService.getModuleAppEngineEnvironment(module).get());
  }

  public void testGetAppEngineArtifactEnvironment_Flexible() {
    Module module = createModule("myModule");
    addAppEngineFlexibleFacet(module);

    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(module).get());
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

  private void addAppEngineStandardFacet(final Module module) {
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        FacetManager.getInstance(module).addFacet(
            new AppEngineStandardFacetType(), "Google App Engine Standard", null);
      }
    }.execute();
  }

  private void addAppEngineFlexibleFacet(final Module module) {
    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        FacetManager.getInstance(module).addFacet(
            new AppEngineFlexibleFacetType(), "Google App Engine Flexible", null);
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

  public void testGetServiceNameFromAppYaml() {
    assertEquals("javaService",
        appEngineProjectService.getServiceNameFromAppYaml(
            Paths.get(getTestDataPath().toString(), "java.yaml").toString()).get());
  }

  public void testGetServiceNameFromAppYaml_noService() {
    assertFalse(appEngineProjectService.getServiceNameFromAppYaml(
        Paths.get(getTestDataPath().toString(), "noservice.yaml").toString()).isPresent());
  }

  public void testGetFlexibleRuntimeFromAppYaml_javaRuntime() {
    assertEquals(FlexibleRuntime.java,
        appEngineProjectService.getFlexibleRuntimeFromAppYaml(
            Paths.get(getTestDataPath().toString(), "java.yaml").toString()).get());
  }

  public void testGetFlexibleRuntimeFromAppYaml_customRuntime() {
    assertEquals(FlexibleRuntime.custom,
        appEngineProjectService.getFlexibleRuntimeFromAppYaml(
            Paths.get(getTestDataPath().toString(), "custom.yaml").toString()).get());
  }

  public void testGetFlexibleRuntimeFromAppYaml_irregularFormat() throws IOException {
    assertEquals(FlexibleRuntime.custom,
        appEngineProjectService.getFlexibleRuntimeFromAppYaml(
            createTempFile("app.yaml", "   runtime :    custom ").getAbsolutePath()).get());
  }

  public void testGetServiceNameFromAppEngineWebXml() {
    assertEquals("java8Service",
        appEngineProjectService.getServiceNameFromAppEngineWebXml(
            loadTestWebXml("testData/descriptor/appengine-web_runtime-java8.xml")));
  }

  public void testGetServiceNameFromAppEngineWebXml_module() {
    assertEquals("java8Service",
        appEngineProjectService.getServiceNameFromAppEngineWebXml(
            loadTestWebXml("testData/descriptor/appengine-web_runtime-java8-module.xml")));
  }

  public void testGetServiceNameFromAppEngineWebXml_serviceAndModule() {
    assertEquals("java8Service",
        appEngineProjectService.getServiceNameFromAppEngineWebXml(
            loadTestWebXml(
                "testData/descriptor/appengine-web_runtime-java8-serviceandmodule.xml")));
  }

  public static File getTestDataPath() {
    try {
      URL resource = AppEngineCodeInsightTestCase.class.getResource("/project");
      return Paths.get(resource.toURI()).toFile();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
