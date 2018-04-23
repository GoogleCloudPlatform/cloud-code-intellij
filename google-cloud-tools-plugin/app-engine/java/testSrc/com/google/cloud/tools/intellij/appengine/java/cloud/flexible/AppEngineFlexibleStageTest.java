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

package com.google.cloud.tools.intellij.appengine.java.cloud.flexible;

import static com.google.cloud.tools.intellij.testing.TestUtils.expectThrows;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.intellij.appengine.java.cloud.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestDirectory;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

@RunWith(JUnit4.class)
public final class AppEngineFlexibleStageTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private LoggingHandler mockLoggingHandler;

  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module javaModule;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module customModule;

  @TestModule(facetTypeId = AppEngineFlexibleFacetType.STRING_ID)
  private Module noYamlModule;

  @TestFile(name = "java.yaml", contents = "runtime: java")
  private File javaYaml;

  @TestFile(name = "custom.yaml", contents = "runtime: custom")
  private File customYaml;

  @TestFile(name = "Dockerfile")
  private File dockerfile;

  @TestFile(name = "artifact.war")
  private File warArtifact;

  @TestFile(name = "artifact.jar")
  private File jarArtifact;

  @TestFile(name = "unknown")
  private File unknownArtifact;

  @TestDirectory(name = "stagingDirectory")
  private File stagingDirectory;

  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private AppEngineFlexibleStage stage;

  @Before
  public void setUp() {
    FacetManager.getInstance(javaModule)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath(javaYaml.getPath());
    FacetManager.getInstance(customModule)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath(customYaml.getPath());
    FacetManager.getInstance(noYamlModule)
        .getFacetByType(AppEngineFlexibleFacet.getFacetType().getId())
        .getConfiguration()
        .setAppYamlPath("some/invalid/path");

    deploymentConfiguration = new AppEngineDeploymentConfiguration();
    deploymentConfiguration.setModuleName(javaModule.getName());

    stage =
        new AppEngineFlexibleStage(
            mockLoggingHandler,
            warArtifact.toPath(),
            deploymentConfiguration,
            testFixture.getProject());
  }

  @Test
  public void stage_noYaml() throws IOException {
    deploymentConfiguration.setModuleName(noYamlModule.getName());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isFalse();
    verify(mockLoggingHandler)
        .print(
            "Error occurred during App Engine flexible staging: the specified app.yaml "
                + "configuration file does not exist or is not a valid file.\n");
  }

  @Test
  public void stage_noDockerDirectory() throws IOException {
    deploymentConfiguration.setModuleName(customModule.getName());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isFalse();
    verify(mockLoggingHandler)
        .print(
            "Error occurred during App Engine flexible staging: no Dockerfile directory was "
                + "specified.\n");
  }

  @Test
  public void stage_noDockerfile() throws IOException {
    Path directory = Files.createTempDirectory("noDockerfile");
    AppEngineFlexibleFacet.getFacetByModule(customModule)
        .getConfiguration()
        .setDockerDirectory(directory.toString());
    deploymentConfiguration.setModuleName(customModule.getName());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isFalse();
    verify(mockLoggingHandler)
        .print(
            "Error occurred during App Engine flexible staging: there is no Dockerfile in "
                + "specified directory.\n");
  }

  @Test
  public void stage_noModule() throws IOException {
    deploymentConfiguration.setModuleName(null);

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isFalse();
    verify(mockLoggingHandler)
        .print(
            "Error occurred during App Engine flexible staging: no app.yaml configuration file was "
                + "specified.\n");
  }

  @Test
  public void stage_withIOException_doesPropagateException() {
    deploymentConfiguration.setModuleName(javaModule.getName());

    // Throws an IOException because the staging directory given is actually a file. No validation
    // occurs on the exception message because it is OS dependent and can vary.
    expectThrows(IOException.class, () -> stage.stage(dockerfile.toPath()));
  }

  @Test
  public void stage_javaRuntime() throws IOException {
    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertFileIsStaged(javaYaml);
    assertFileIsStaged(warArtifact);
  }

  @Test
  public void stage_customRuntime() throws IOException {
    AppEngineFlexibleFacet.getFacetByModule(customModule)
        .getConfiguration()
        .setDockerDirectory(dockerfile.getParent());
    deploymentConfiguration.setModuleName(customModule.getName());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertFileIsStaged(customYaml);
    assertFileIsStaged(dockerfile);
    assertFileIsStaged(warArtifact);
  }

  @Test
  public void stage_withJarArtifact_doesStageTargetJar() throws IOException {
    stage =
        new AppEngineFlexibleStage(
            mockLoggingHandler,
            jarArtifact.toPath(),
            deploymentConfiguration,
            testFixture.getProject());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertFileIsStaged(jarArtifact);
  }

  @Test
  public void stage_withStagedArtifactName_doesStageChosenArtifactName() throws IOException {
    String stagedArtifactName = "some-artifact.war";
    deploymentConfiguration.setStagedArtifactName(stagedArtifactName);

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertThat(Files.exists(stagingDirectory.toPath().resolve(stagedArtifactName))).isTrue();
  }

  @Test
  public void stage_withDirectoriesInStagedArtifactName_doesCreateDirectories() throws IOException {
    String stagedArtifactName = "some/directory/artifact.war";
    deploymentConfiguration.setStagedArtifactName(stagedArtifactName);

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertThat(Files.exists(stagingDirectory.toPath().resolve(stagedArtifactName))).isTrue();
  }

  @Test
  public void stage_withLegacyStagedNameBit_doesStageTargetWar() throws IOException {
    deploymentConfiguration.setStagedArtifactNameLegacy(true);

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertThat(Files.exists(stagingDirectory.toPath().resolve("target.war"))).isTrue();
  }

  @Test
  public void stage_withJarArtifact_andLegacyStagedNameBit_doesStageTargetJar() throws IOException {
    deploymentConfiguration.setStagedArtifactNameLegacy(true);
    stage =
        new AppEngineFlexibleStage(
            mockLoggingHandler,
            jarArtifact.toPath(),
            deploymentConfiguration,
            testFixture.getProject());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertThat(Files.exists(stagingDirectory.toPath().resolve("target.jar"))).isTrue();
  }

  @Test
  public void stage_withUnknownArtifact_andLegacyStagedNameBit_doesStageTarget()
      throws IOException {
    deploymentConfiguration.setStagedArtifactNameLegacy(true);
    stage =
        new AppEngineFlexibleStage(
            mockLoggingHandler,
            unknownArtifact.toPath(),
            deploymentConfiguration,
            testFixture.getProject());

    boolean result = stage.stage(stagingDirectory.toPath());

    assertThat(result).isTrue();
    assertThat(Files.exists(stagingDirectory.toPath().resolve("target"))).isTrue();
  }

  /** Asserts the given {@link File} was staged in the staging directory. */
  private void assertFileIsStaged(File file) {
    assertThat(Files.exists(stagingDirectory.toPath().resolve(file.getName()))).isTrue();
  }
}
