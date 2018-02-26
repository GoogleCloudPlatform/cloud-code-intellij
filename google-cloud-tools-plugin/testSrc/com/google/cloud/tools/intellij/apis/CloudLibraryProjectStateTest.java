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
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClient;
import com.google.cloud.tools.intellij.testing.apis.TestCloudLibrary.TestCloudLibraryClientMavenCoordinates;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;
import org.jetbrains.idea.maven.wizards.MavenModuleBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link CloudLibraryProjectState}. */
public class CloudLibraryProjectStateTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @Mock @TestService CloudLibrariesService librariesService;
  @TestModule private Module module;
  private Module testModule;

  @TestDirectory(name = "root")
  private File root;

  private static final TestCloudLibraryClientMavenCoordinates JAVA_CLIENT_MAVEN_COORDS_1 =
      TestCloudLibraryClientMavenCoordinates.create("java", "client-1", "1.0.0");
  private static final TestCloudLibraryClient JAVA_CLIENT_1 =
      TestCloudLibraryClient.create(
          "Client 1",
          "java",
          "API Ref 1",
          "alpha",
          "Source 1",
          "Lang Level 1",
          JAVA_CLIENT_MAVEN_COORDS_1);
  private static final TestCloudLibrary LIBRARY_1 =
      TestCloudLibrary.create(
          "Library 1",
          "ID 1",
          "service_1",
          "Docs Link 1",
          "Description 1",
          "Icon Link 1",
          ImmutableList.of(JAVA_CLIENT_1));

  private CloudLibraryProjectState state;
  private MavenModuleBuilder moduleBuilder;
  private MavenId id;

  @Before
  public void setUp() {
    state = CloudLibraryProjectState.getInstance(testFixture.getProject());
    moduleBuilder = new MavenModuleBuilder();
    id = new MavenId("org.foo", "module", "1.0");

    try {
      setModuleNameAndRoot("module", testFixture.getProject().getBasePath());
    } catch (Exception e) {
      e.printStackTrace();
    }

    //    initMavenModule();
  }

  private void setModuleNameAndRoot(String name, String root) {
    moduleBuilder.setName(name);
    moduleBuilder.setModuleFilePath(root + "/" + name + ".iml");
    moduleBuilder.setContentEntryPath(root);
  }

  @Test
  public void managedLibraries_withNoDependenies_isEmptyOptional() {
    //    assertThat(state.getManagedLibraries(module).isPresent()).isFalse();
    createNewModule(id);
    assertThat(state.getManagedLibraries(module)).isEmpty();
    // todo needs to be tested _before_ the maven module is created (move creation to each test)
  }

  @Test
  public void managedLibraries_whenProjectHasManagedLibraries_isPresent() {
    when(librariesService.getCloudLibraries())
        .thenReturn(ImmutableList.of(LIBRARY_1.toCloudLibrary()));

    //        state.syncManagedProjectLibraries();
  }

  // todo test when no maven module
  // todo test when only maven module
  // todo test with both maven and non-maven modules
  // todo test with maven module with empty/no dependency section

  private void createNewModule(MavenId id) {
    moduleBuilder.setProjectId(id);

    new WriteAction() {
      protected void run(@NotNull Result result) throws Throwable {
        ModifiableModuleModel model =
            ModuleManager.getInstance(testFixture.getProject()).getModifiableModel();
        moduleBuilder.createModule(model);
        model.commit();
      }
    }.execute();

    resolveDependenciesAndImport();
  }

  private void resolveDependenciesAndImport() {
    //    UIUtil.invokeAndWaitIfNeeded((Runnable)() -> {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              ApplicationManager.getApplication()
                  .runWriteAction(
                      () -> {
                        MavenProjectsManager myProjectsManager =
                            MavenProjectsManager.getInstance(testFixture.getProject());
                        myProjectsManager.waitForResolvingCompletion();
                        myProjectsManager.performScheduledImportInTests();
                      });
            },
            ModalityState.NON_MODAL);
  }

  private void initMavenModule() {
    ApplicationManager.getApplication()
        .invokeAndWait(
            () -> {
              VirtualFile pomVirtualFile = createAndAddPomToModule();

              MavenProjectsManager.getInstance(testFixture.getProject()).initForTests();
              MavenProjectsManager.getInstance(testFixture.getProject())
                  .getProjectsTreeForTests()
                  .update(
                      ImmutableList.of(pomVirtualFile),
                      false,
                      new MavenGeneralSettings(),
                      new MavenProgressIndicator());
            });
  }

  private VirtualFile createAndAddPomToModule() {
    return ApplicationManager.getApplication()
        .runWriteAction(
            new Computable<VirtualFile>() {
              @Override
              public VirtualFile compute() {
                ModifiableRootModel modifiableModel =
                    ModuleRootManager.getInstance(module).getModifiableModel();
                VirtualFile rootVirtualDir = LocalFileSystem.getInstance().findFileByIoFile(root);
                VirtualFile pomVirtualFile = null;
                try {

                  pomVirtualFile = rootVirtualDir.createChildData(this, "pom.xml");

                  // copy data from template test pom to the pom VirtualFile
                  Path templatePom = Paths.get(getMavenTestDataPath().toString(), "pom.xml");
                  pomVirtualFile.setBinaryContent(Files.readAllBytes(templatePom));
                } catch (IOException e) {
                }

                modifiableModel.addContentEntry(rootVirtualDir);
                modifiableModel.commit();
                return pomVirtualFile;
              }
            });
  }

  private static File getMavenTestDataPath() {
    try {
      URL resource = CloudLibraryProjectStateTest.class.getResource("/maven");
      return Paths.get(resource.toURI()).toFile();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
