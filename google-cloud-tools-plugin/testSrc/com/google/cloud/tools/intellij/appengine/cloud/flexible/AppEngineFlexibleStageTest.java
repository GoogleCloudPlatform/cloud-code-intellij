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

  public void setUp() throws Exception {
    super.setUp();

    loggingHandler = mock(LoggingHandler.class);
    doNothing().when(loggingHandler).print(anyString());

    artifact = createTempFile("artifact.war", "").toPath();
    deploymentConfiguration = new AppEngineDeploymentConfiguration();
    deploymentConfiguration.setYamlPath(createTempFile("custom.yaml", "runtime: custom").getPath());
    deploymentConfiguration.setDockerFilePath(createTempFile("Dockerfile", "").getPath());
    stagingDirectory = createTempDirectory().toPath();
  }

  public void testStage_noYaml() {
    deploymentConfiguration.setYamlPath("I don't exist.");
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration);
    try {
      stage.stage(stagingDirectory);
      fail("No yaml file.");
    } catch (RuntimeException re) {
      assertEquals("The specified YAML configuration file does not exist or is not a valid file.", re.getMessage());
    }
  }

  public void testStage_noDockerfile() {
    deploymentConfiguration.setDockerFilePath("I don't exist.");
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration);
    try {
      stage.stage(stagingDirectory);
      fail("No Dockerfile.");
    } catch (RuntimeException re) {
      assertEquals("The specified Dockerfile configuration file does not exist or is not a valid file.", re.getMessage());
    }
  }

  public void testStage_javaRuntime() throws IOException {
    deploymentConfiguration.setDockerFilePath("I don't exist.");
    deploymentConfiguration.setYamlPath(createTempFile("java.yaml", "runtime: java").getPath());
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration);
    stage.stage(stagingDirectory);
    assertTrue(Files.exists(stagingDirectory.resolve("java.yaml")));
    assertTrue(Files.exists(stagingDirectory.resolve("target.war")));
  }

  public void testStage_customRuntime() {
    AppEngineFlexibleStage stage =
        new AppEngineFlexibleStage(loggingHandler, artifact, deploymentConfiguration);
    stage.stage(stagingDirectory);

    assertTrue(Files.exists(stagingDirectory.resolve("custom.yaml")));
    assertTrue(Files.exists(stagingDirectory.resolve("Dockerfile")));
    assertTrue(Files.exists(stagingDirectory.resolve("target.war")));
  }
}
