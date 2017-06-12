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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import com.intellij.testFramework.PlatformTestCase;

import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AppEngineFlexibleStageTest extends PlatformTestCase {

  @Mock
  private LoggingHandler loggingHandler;

  private Path artifact;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private Path stagingDirectory;
  private Module customModule;
  private String javaModuleName = "java-module";
  private String customModuleName = "custom-module";
  private String noYamlModuleName = "no-yaml";

  @Override
  public void setUp() throws Exception {
    super.setUp();

    loggingHandler = mock(LoggingHandler.class);
    doNothing().when(loggingHandler).print(anyString());

    Module javaModule = createModule(javaModuleName);
    String javaYamlPath = createTempFile("java.yaml", "runtime: java").getPath();
    setupTestFacet(javaModule, javaYamlPath);

    customModule = createModule(customModuleName);
    String customYamlPath = createTempFile("custom.yaml", "runtime: custom").getPath();
    setupTestFacet(customModule, customYamlPath);

    Module noYamlModule = createModule(noYamlModuleName);
    setupTestFacet(noYamlModule, "/i/dont/exist");

    artifact = createTempFile("artifact.war", "").toPath();
    deploymentConfiguration = new AppEngineDeploymentConfiguration();
    deploymentConfiguration.setModuleName(javaModuleName);
    stagingDirectory = createTempDirectory().toPath();
  }

  public void testStage_noYaml() {
    deploymentConfiguration.setModuleName(noYamlModuleName);
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration, getProject());
    try {
      stage.stage(stagingDirectory);
      fail("No yaml file.");
    } catch (RuntimeException re) {
      assertEquals("Error occurred during App Engine flexible staging: the specified app.yaml configuration file does not exist or is not a valid file.", re.getMessage());
    }
  }

  public void testStage_noDockerDirectory() {
    deploymentConfiguration.setModuleName(customModuleName);
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration, getProject());
    try {
      stage.stage(stagingDirectory);
      fail("No Docker directory.");
    } catch (RuntimeException re) {
      assertEquals("Error occured during App Engine flexible staging: there is no Dockerfile in specified directory.", re.getMessage());
    }
  }

  public void testStage_javaRuntime() throws IOException {
    deploymentConfiguration.setModuleName(javaModuleName);
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration, getProject());
    stage.stage(stagingDirectory);
    assertTrue(Files.exists(stagingDirectory.resolve("java.yaml")));
    assertTrue(Files.exists(stagingDirectory.resolve("target.war")));
  }

  public void testStage_customRuntime() throws IOException {
    String dockerDirectory = createTempFile("Dockerfile", "").getParentFile().getPath();
    AppEngineFlexibleFacet.getFacetByModule(customModule)
        .getConfiguration()
        .setDockerDirectory(dockerDirectory);
    deploymentConfiguration.setModuleName(customModuleName);
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration, getProject());
    stage.stage(stagingDirectory);

    assertTrue(Files.exists(stagingDirectory.resolve("custom.yaml")));
    assertTrue(Files.exists(stagingDirectory.resolve("Dockerfile")));
    assertTrue(Files.exists(stagingDirectory.resolve("target.war")));
  }

  private void setupTestFacet(Module module, String yamlPath) {
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> {
              AppEngineFlexibleFacet flexFacet =
                  FacetManager.getInstance(module)
                      .addFacet(
                          AppEngineFlexibleFacet.getFacetType(),
                          "flex facet",
                          null /* underlyingFacet */);
              flexFacet.getConfiguration().setAppYamlPath(yamlPath);
            });
  }
}
