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

package com.google.cloud.tools.intellij.apis;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.MavenTestUtils;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link CloudLibraryDependencyWriter}. */
public class CloudLibraryDependencyWriterTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  private static final String CLOUD_LIB_GROUP_ID = "test-java";
  private static final String CLOUD_LIB_ARTIFACT_ID = "com.test.java";
  private static final String CLOUD_LIB_VERSION = "1.0.0";
  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS =
      TestCloudLibraryClientMavenCoordinates.create(
          CLOUD_LIB_GROUP_ID, CLOUD_LIB_ARTIFACT_ID, CLOUD_LIB_VERSION);
  private static final TestCloudLibraryClient JAVA_CLIENT =
      TestCloudLibraryClient.create(
          "Client", "java", "API Ref", "alpha", "Source", "Lang Level", JAVA_CLIENT_MAVEN_COORDS);
  private static final TestCloudLibrary LIBRARY =
      TestCloudLibrary.create(
          "Library",
          "ID",
          "service",
          "Docs Link",
          "Description",
          "Icon Link 1",
          ImmutableList.of(JAVA_CLIENT));

  private MavenModuleBuilder moduleBuilder;

  @Before
  public void setUp() {
    moduleBuilder = MavenTestUtils.getInstance().initMavenModuleBuilder(testFixture.getProject());
  }

  @Test
  public void addLibraries_withNoBomSelected_writesDependencyWithVersion() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              MavenDomProjectModel model = createMavenModelWithCloudLibrary(null /*bomVersion*/);

              List<MavenDomDependency> cloudLibraryDependencies =
                  model.getDependencies().getDependencies();
              assertThat(cloudLibraryDependencies.size()).isEqualTo(1);

              MavenDomDependency dependency = cloudLibraryDependencies.get(0);
              assertThat(dependency.getGroupId().getStringValue()).isEqualTo(CLOUD_LIB_GROUP_ID);
              assertThat(dependency.getArtifactId().getStringValue())
                  .isEqualTo(CLOUD_LIB_ARTIFACT_ID);
              assertThat(dependency.getVersion().getStringValue()).isEqualTo(CLOUD_LIB_VERSION);
            });
  }

  @Test
  public void addLibraries_withBomSelected_writesDependencyWithNoVersion() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              String bomVersion = "1.0.0-beta";
              MavenDomProjectModel model = createMavenModelWithCloudLibrary(bomVersion);

              List<MavenDomDependency> cloudLibraryDependencies =
                  model.getDependencies().getDependencies();
              assertThat(cloudLibraryDependencies.size()).isEqualTo(1);

              MavenDomDependency dependency = cloudLibraryDependencies.get(0);
              assertThat(dependency.getGroupId().getStringValue()).isEqualTo(CLOUD_LIB_GROUP_ID);
              assertThat(dependency.getArtifactId().getStringValue())
                  .isEqualTo(CLOUD_LIB_ARTIFACT_ID);
              assertThat(dependency.getVersion().getStringValue()).isNull();
            });
  }

  @Test
  public void addLibraries_withNoBomSelected_doesNotWriteBom() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              MavenDomProjectModel model = createMavenModelWithCloudLibrary(null /*bomVersion*/);

              assertThat(model.getDependencyManagement().getDependencies().getDependencies())
                  .isEmpty();
            });
  }

  @Test
  public void addLibraries_withBomSelected_writesBom() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              String bomVersion = "1.0.0-beta";
              MavenDomProjectModel model = createMavenModelWithCloudLibrary(bomVersion);

              List<MavenDomDependency> dependencyManagementDependencies =
                  model.getDependencyManagement().getDependencies().getDependencies();
              assertThat(dependencyManagementDependencies.size()).isEqualTo(1);

              MavenDomDependency bomDependency = dependencyManagementDependencies.get(0);
              assertThat(bomDependency.getGroupId().getStringValue()).isEqualTo("com.google.cloud");
              assertThat(bomDependency.getArtifactId().getStringValue())
                  .isEqualTo("google-cloud-bom");
              assertThat(bomDependency.getScope().getStringValue()).isEqualTo("import");
              assertThat(bomDependency.getType().getStringValue()).isEqualTo("pom");
              assertThat(bomDependency.getVersion().getStringValue()).isEqualTo(bomVersion);
            });
  }

  private MavenDomProjectModel createMavenModelWithCloudLibrary(@Nullable String bomVersion) {
    Module module =
        MavenTestUtils.getInstance().createNewMavenModule(moduleBuilder, testFixture.getProject());

    CloudLibraryDependencyWriter.addLibraries(
        ImmutableSet.of(LIBRARY.toCloudLibrary()), module, bomVersion);

    MavenProject mavenProject =
        MavenProjectsManager.getInstance(testFixture.getProject()).findProject(module);
    return MavenDomUtil.getMavenDomProjectModel(testFixture.getProject(), mavenProject.getFile());
  }
}
