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

package com.google.cloud.tools.intellij.cloudapis;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.MavenTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.cloud.tools.libraries.json.CloudLibrary;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.util.xml.DomUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link CloudLibraryProjectState}. */
public class CloudLibraryProjectStateTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @Mock @TestService CloudLibrariesService librariesService;

  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS =
      TestCloudLibraryClientMavenCoordinates.create("java", "client", "1.0.0");
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

  private CloudLibraryProjectState state;

  @Before
  public void setUp() {
    state = CloudLibraryProjectState.getInstance(testFixture.getProject());
  }

  @After
  public void tearDown() {
    MavenServerManager.getInstance().shutdown(true);
  }

  @Test
  public void getCloudLibraries_withNoDependencies_isEmpty() {
    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            module -> assertThat(state.getCloudLibraries(module)).isEmpty());
  }

  @Test
  public void getCloudLibraries_withMalformedPom_isEmpty() {
    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            module -> {
              ApplicationManager.getApplication()
                  .runWriteAction(
                      () -> {
                        try {
                          MavenProject mavenProject =
                              MavenProjectsManager.getInstance(testFixture.getProject())
                                  .findProject(module);
                          mavenProject
                              .getFile()
                              .setBinaryContent(
                                  "not a valid pom.xml".getBytes(Charset.defaultCharset()));
                        } catch (IOException e) {
                          throw new AssertionError("failed to write test content to pom.xml", e);
                        }
                      });

              state.syncManagedProjectLibraries();
            });
  }

  @Test
  public void getCloudLibraries_withOnlyNonCloudDependencies_isEmpty() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            module -> {
              MavenId nonCloudDependency = new MavenId("my-group", "my-artifact", "1.0");
              writeDependenciesToPom(module, ImmutableList.of(nonCloudDependency));

              assertThat(state.getCloudLibraries(module)).isEmpty();
            });
  }

  @Test
  public void getCloudLibraries_withCloudAndNonCloudDependencies_returnsValidCloudLibrary() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            module -> {
              TestCloudLibraryClientMavenCoordinates mavenCoordinates =
                  LIBRARY.clients().get(0).mavenCoordinates();
              String groupId = mavenCoordinates.groupId();
              String artifactId = mavenCoordinates.artifactId();

              MavenId cloudDependency = new MavenId(groupId, artifactId, "1.0");
              MavenId nonCloudDependency = new MavenId("my-group", "my-artifact", "1.0");
              writeDependenciesToPom(module, ImmutableList.of(cloudDependency, nonCloudDependency));

              state.syncManagedProjectLibraries();

              Set<CloudLibrary> libraries = state.getCloudLibraries(module);
              assertThat_cloudLibrariesContainsExactlyOne(libraries);
            });
  }

  @Test
  public void getCloudLibraries_withOnlyCloudDependencies_returnsValidCloudLibrary() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            module -> {
              TestCloudLibraryClientMavenCoordinates mavenCoordinates =
                  LIBRARY.clients().get(0).mavenCoordinates();
              String groupId = mavenCoordinates.groupId();
              String artifactId = mavenCoordinates.artifactId();

              MavenId managedDependency = new MavenId(groupId, artifactId, "1.0");
              writeDependenciesToPom(module, ImmutableList.of(managedDependency));

              state.syncManagedProjectLibraries();

              Set<CloudLibrary> libraries = state.getCloudLibraries(module);
              assertThat_cloudLibrariesContainsExactlyOne(libraries);
            });
  }

  private void assertThat_cloudLibrariesContainsExactlyOne(Set<CloudLibrary> cloudLibraries) {
    assertThat(cloudLibraries.size()).isEqualTo(1);
    CloudLibrary library = cloudLibraries.iterator().next();
    assertThat(library.getName()).isEqualTo(LIBRARY.name());
  }

  private void writeDependenciesToPom(Module module, List<MavenId> dependencies) {
    MavenProject mavenProject =
        MavenProjectsManager.getInstance(testFixture.getProject()).findProject(module);
    MavenDomProjectModel model =
        MavenDomUtil.getMavenDomProjectModel(testFixture.getProject(), mavenProject.getFile());

    new WriteCommandAction(testFixture.getProject(), DomUtil.getFile(model)) {
      @Override
      protected void run(@NotNull Result result) {
        dependencies.forEach(
            dependency -> MavenDomUtil.createDomDependency(model, null, dependency));
      }
    }.execute();
  }
}
