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
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestDirectory;
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
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Computable;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.util.xml.DomUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;
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

  @TestDirectory(name = "root")
  private File root;

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
  private MavenModuleBuilder moduleBuilder;
  private MavenId id;

  @Before
  public void setUp() {
    state = CloudLibraryProjectState.getInstance(testFixture.getProject());
    moduleBuilder = new MavenModuleBuilder();
    id = new MavenId("org.foo", "module", "1.0");

    setModuleNameAndRoot(testFixture.getProject().getBasePath());
  }

  @After
  public void tearDown() {
    MavenServerManager.getInstance().shutdown(true);
  }

  @Test
  public void getCloudLibraries_withNoDependencies_isEmpty() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              Module module = createNewModule(id);

              assertThat(state.getCloudLibraries(module)).isEmpty();
            });
  }

  @Test
  public void getCloudLibraries_withOnlyNonCloudDependencies_isEmpty() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              Module module = createNewModule(id);
              MavenId nonCloudDependency = new MavenId("my-group", "my-artifact", "1.0");
              writeDependenciesToPom(module, ImmutableList.of(nonCloudDependency));

              assertThat(state.getCloudLibraries(module)).isEmpty();
            });
  }

  @Test
  public void getCloudLibraries_withCloudAndNonCloudDependencies_returnsValidCloudLibrary() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              Module module = createNewModule(id);

              TestCloudLibraryClientMavenCoordinates mavenCoordinates =
                  LIBRARY.clients().get(0).mavenCoordinates();
              String groupId = mavenCoordinates.groupId();
              String artifactId = mavenCoordinates.artifactId();

              MavenId cloudDependency = new MavenId(groupId, artifactId, "1.0");
              MavenId nonCloudDependency = new MavenId("my-group", "my-artifact", "1.0");
              writeDependenciesToPom(module, ImmutableList.of(cloudDependency, nonCloudDependency));

              state.syncManagedProjectLibraries();

              Set<CloudLibrary> libraries = state.getCloudLibraries(module);
              assertThat_managedLibrariesContainsExactlyOne(libraries);
            });
  }

  @Test
  public void getCloudLibraries_withOnlyCloudDependencies_returnsValidCloudLibrary() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY.toCloudLibrary()));

    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              Module module = createNewModule(id);

              TestCloudLibraryClientMavenCoordinates mavenCoordinates =
                  LIBRARY.clients().get(0).mavenCoordinates();
              String groupId = mavenCoordinates.groupId();
              String artifactId = mavenCoordinates.artifactId();

              MavenId managedDependency = new MavenId(groupId, artifactId, "1.0");
              writeDependenciesToPom(module, ImmutableList.of(managedDependency));

              state.syncManagedProjectLibraries();

              Set<CloudLibrary> libraries = state.getCloudLibraries(module);
              assertThat_managedLibrariesContainsExactlyOne(libraries);
            });
  }

  private void assertThat_managedLibrariesContainsExactlyOne(Set<CloudLibrary> managedLibraries) {
    assertThat(managedLibraries.size()).isEqualTo(1);
    CloudLibrary library = managedLibraries.iterator().next();
    assertThat(library.getName()).isEqualTo(LIBRARY.name());
  }

  private void setModuleNameAndRoot(String root) {
    moduleBuilder.setName("module");
    moduleBuilder.setModuleFilePath(root + "/module.iml");
    moduleBuilder.setContentEntryPath(root);
  }

  private Module createNewModule(MavenId id) {
    moduleBuilder.setProjectId(id);

    return ApplicationManager.getApplication()
        .runWriteAction(
            (Computable<Module>)
                () -> {
                  ModifiableModuleModel model =
                      ModuleManager.getInstance(testFixture.getProject()).getModifiableModel();
                  Module module;
                  try {
                    module = moduleBuilder.createModule(model);
                  } catch (IOException
                      | ModuleWithNameAlreadyExists
                      | JDOMException
                      | ConfigurationException e) {
                    throw new AssertionError("Error creating Mavenized module");
                  }
                  model.commit();

                  resolveDependenciesAndImport();
                  return module;
                });
  }

  private void resolveDependenciesAndImport() {
    MavenProjectsManager myProjectsManager =
        MavenProjectsManager.getInstance(testFixture.getProject());
    myProjectsManager.waitForResolvingCompletion();
    myProjectsManager.performScheduledImportInTests();
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
