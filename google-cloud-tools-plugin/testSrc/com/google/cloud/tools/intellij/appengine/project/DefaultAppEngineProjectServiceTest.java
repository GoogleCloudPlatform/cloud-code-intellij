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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardRuntime;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for {@link DefaultAppEngineProjectService}
 */
public class DefaultAppEngineProjectServiceTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @TestFixture private IdeaProjectTestFixture testFixture;

  private @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID) Module flexModule;
  private @TestModule(facetTypeId = AppEngineStandardFacetType.STRING_ID) Module standardModule;


  private @TestFile(name = "app.yaml", contents = "   runtime :    custom ") File
      appYamlCustomRuntime;

  private @TestFile(name = "app.yaml", contents = "runtime: custom\nenv_variables:\n  'DBG_ENAB")
  File appYamlCustomRuntimeWithEnvVars;

  private DefaultAppEngineProjectService appEngineProjectService;

  @Before
  public void setUp() throws Exception {
    // Fixes https://youtrack.jetbrains.com/issue/IDEA-129297. Only occurs in Jenkins.
//    VfsRootAccess.allowRootAccess(System.getProperty("user.dir"));
    appEngineProjectService = new DefaultAppEngineProjectService();
  }

  @Test
  public void testGetAppEngineArtifactEnvironment_Standard() {
    assertEquals(AppEngineEnvironment.APP_ENGINE_STANDARD,
        appEngineProjectService.getModuleAppEngineEnvironment(standardModule).get());
  }

  @Test
  public void testGetAppEngineArtifactEnvironment_Flexible() {
    assertEquals(AppEngineEnvironment.APP_ENGINE_FLEX,
        appEngineProjectService.getModuleAppEngineEnvironment(flexModule).get());
  }

  @Test
  public void testGetAppEngineStandardDeclaredRuntime_NullArg() {
    assertNull(appEngineProjectService.getAppEngineStandardDeclaredRuntime(null));
  }

  @Test
  public void testGetAppEngineStandardDeclaredRuntime_NoneDeclared() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              XmlFile appEngineWebXml = loadTestWebXml("testData/descriptor/appengine-web.xml");
              assertNull(
                  appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
            });
  }

  @Test
  public void testGetAppEngineStandardDeclaredRuntime_Java8() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              XmlFile appEngineWebXml =
                  loadTestWebXml("testData/descriptor/appengine-web_runtime-java8.xml");
              assertEquals(
                  AppEngineStandardRuntime.JAVA_8,
                  appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
            });
  }

  @Test
  public void testGetAppEngineStandardDeclaredRuntime_Invalid() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              XmlFile appEngineWebXml =
                  loadTestWebXml("testData/descriptor/appengine-web_runtime-invalid.xml");
              assertNull(
                  appEngineProjectService.getAppEngineStandardDeclaredRuntime(appEngineWebXml));
            });
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
        : (XmlFile) PsiManager.getInstance(testFixture.getProject()).findFile(vFile);
  }

  @Test
  public void testGetServiceNameFromAppYaml() throws MalformedYamlFileException {
    assertEquals("javaService",
        appEngineProjectService.getServiceNameFromAppYaml(
            Paths.get(getTestDataPath().toString(), "java.yaml").toString()).get());
  }

  @Test
  public void testGetServiceNameFromAppYaml_noService() throws MalformedYamlFileException {
    assertFalse(appEngineProjectService.getServiceNameFromAppYaml(
        Paths.get(getTestDataPath().toString(), "noservice.yaml").toString()).isPresent());
  }

  @Test
  public void testGetFlexibleRuntimeFromAppYaml_javaRuntime() throws MalformedYamlFileException {
    assertEquals(FlexibleRuntime.JAVA,
        appEngineProjectService.getFlexibleRuntimeFromAppYaml(
            Paths.get(getTestDataPath().toString(), "java.yaml").toString()).get());
  }

  @Test
  public void testGetFlexibleRuntimeFromAppYaml_customRuntime() throws MalformedYamlFileException {
    assertEquals(FlexibleRuntime.CUSTOM,
        appEngineProjectService.getFlexibleRuntimeFromAppYaml(
            Paths.get(getTestDataPath().toString(), "custom.yaml").toString()).get());
  }

  @Test
  public void testGetFlexibleRuntimeFromAppYaml_irregularFormatButValid()
      throws IOException, MalformedYamlFileException {
    assertEquals(
        FlexibleRuntime.CUSTOM,
        appEngineProjectService
            .getFlexibleRuntimeFromAppYaml(appYamlCustomRuntime.getAbsolutePath())
            .get());
  }


  @Test
  public void testGetFlexibleRuntimeFromAppYaml_malformedYaml() throws IOException {
    try {
      assertEquals(
          FlexibleRuntime.CUSTOM,
          appEngineProjectService
              .getFlexibleRuntimeFromAppYaml(appYamlCustomRuntimeWithEnvVars.getAbsolutePath())
              .get());
      fail("YAML is malformed.");
    } catch (MalformedYamlFileException myf) {
      // Success.
    }
  }

  @Test
  public void testGetServiceNameFromAppEngineWebXml() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertEquals(
                  "java8Service",
                  appEngineProjectService.getServiceNameFromAppEngineWebXml(
                      loadTestWebXml("testData/descriptor/appengine-web_runtime-java8.xml")));
            });
  }

  @Test
  public void testGetServiceNameFromAppEngineWebXml_module() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertEquals(
                  "java8Service",
                  appEngineProjectService.getServiceNameFromAppEngineWebXml(
                      loadTestWebXml(
                          "testData/descriptor/appengine-web_runtime-java8-module.xml")));
            });
  }

  @Test
  public void testGetServiceNameFromAppEngineWebXml_serviceAndModule() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              assertEquals(
                  "java8Service",
                  appEngineProjectService.getServiceNameFromAppEngineWebXml(
                      loadTestWebXml(
                          "testData/descriptor/appengine-web_runtime-java8-serviceandmodule.xml")));
            });

  }

  @Test
  public void testGenerateAppYaml() {
    Path outputDir = Paths.get(testFixture.getProject().getBasePath() + "/src/main/appengine");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              appEngineProjectService.generateAppYaml(
                  FlexibleRuntime.JAVA, testFixture.getModule(), outputDir);

              File[] listOfFiles = outputDir.toFile().listFiles();
              Assert.assertEquals(1, listOfFiles.length);
              Assert.assertEquals("app.yaml", listOfFiles[0].getName());
            });
  }

  @Test
  public void testGenerateDockerfile_war() {
    Path outputDir = Paths.get(testFixture.getProject().getBasePath() + "/src/main/docker");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              appEngineProjectService.generateDockerfile(
                  AppEngineFlexibleDeploymentArtifactType.WAR, testFixture.getModule(), outputDir);

              File[] listOfFiles = outputDir.toFile().listFiles();
              Assert.assertEquals(1, listOfFiles.length);
              Assert.assertEquals("Dockerfile", listOfFiles[0].getName());
            });
  }

  @Test
  public void testGenerateDockerfile_jar() {
    Path outputDir = Paths.get(testFixture.getProject().getBasePath() + "/src/main/docker");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              appEngineProjectService.generateDockerfile(
                  AppEngineFlexibleDeploymentArtifactType.JAR, testFixture.getModule(), outputDir);

              File[] listOfFiles = outputDir.toFile().listFiles();
              Assert.assertEquals(1, listOfFiles.length);
              Assert.assertEquals("Dockerfile", listOfFiles[0].getName());
            });
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
