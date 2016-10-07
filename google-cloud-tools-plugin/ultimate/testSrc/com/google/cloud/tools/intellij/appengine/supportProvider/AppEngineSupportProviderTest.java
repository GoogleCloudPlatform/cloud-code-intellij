/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.appengine.supportProvider;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.facet.AppEngineFacet;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineFrameworkType;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineStandardLibraryPanel;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineSupportProvider;
import com.google.cloud.tools.intellij.appengine.facet.AppEngineSupportProvider.AppEngineSupportConfigurable;
import com.google.cloud.tools.intellij.appengine.facet.MavenRepositoryLibraryDownloader;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.server.run.AppEngineServerConfigurationType;
import com.google.cloud.tools.intellij.compiler.artifacts.ArtifactsTestUtil;
import com.google.cloud.tools.intellij.javaee.supportProvider.JavaeeFrameworkSupportProviderTestCase;

import com.intellij.appengine.AppEngineCodeInsightTestCase;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.javaee.model.enums.WebAppVersion;
import com.intellij.javaee.run.configuration.CommonModel;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.framework.WebFrameworkType;
import com.intellij.javaee.web.framework.WebFrameworkVersion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryDescription;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.util.List;

/**
 * @author nik
 */
public class AppEngineSupportProviderTest extends JavaeeFrameworkSupportProviderTestCase {
  public void testAppEngine_noManagedLibrariesSelected() {
    setupAppEngine(new AppEngineStandardLibraryPanel(false /*enabled*/), null /*library*/);
    addSupport();

    assertNull(FacetManager.getInstance(myModule).getFacetByType(WebFacet.ID));
    final String moduleName = myModule.getName();
    ArtifactsTestUtil.assertLayout(myProject, moduleName, "<root>\n" +
                                                          " WEB-INF/\n" +
                                                          "  classes/\n" +
                                                          "   module:" + moduleName + "\n");
  }

  public void testAppEngineWithWeb_noManagedLibrariesSelected() {
    setupAppEngine(new AppEngineStandardLibraryPanel(false /*enabled*/), null /*library*/);
    selectFramework(WebFacet.ID);
    selectVersion(WebFrameworkType.getInstance(), new WebFrameworkVersion(WebAppVersion.WebAppVersion_2_5));
    addSupport();

    getFacet(AppEngineFacet.ID);
    assertFileExist("web/WEB-INF/web.xml");
    assertFileExist("web/WEB-INF/appengine-web.xml");

    final String moduleName = myModule.getName();
    Artifact artifact = ArtifactsTestUtil.findArtifact(myProject, moduleName + ":war exploded");
    ArtifactsTestUtil.assertLayout(artifact.getRootElement(), "<root>\n" +
                                                              " javaee-resources:Web(" + moduleName + ")\n" +
                                                              " WEB-INF/\n" +
                                                              "  classes/\n" +
                                                              "   module:" + moduleName + "\n");
    assertRunConfigurationCreated(artifact);
  }

  public void testAppEngine_defaultManagedLibrariesSelected() {
    AppEngineStandardLibraryPanel libraryPanel = new AppEngineStandardLibraryPanel(true /*enabled*/);

    LibraryEx library = mock(LibraryEx.class);
    when(library.getTable()).thenReturn(ProjectLibraryTable.getInstance(myModule.getProject()));
    when(library.getExcludedRoots()).thenReturn(new VirtualFile[0]);
    when(library.getName()).thenReturn("javax.servlet:servlet-api:2.5");

    setupAppEngine(libraryPanel, library);
    addSupport();

    assertNull(FacetManager.getInstance(myModule).getFacetByType(WebFacet.ID));
    final String moduleName = myModule.getName();
    ArtifactsTestUtil.assertLayout(myProject, moduleName, "<root>\n" +
                                                          " WEB-INF/\n" +
                                                          "  classes/\n" +
                                                          "   module:" + moduleName + "\n" +
                                                          "  lib/\n" +
                                                          "   lib:javax.servlet:servlet-api:2.5(project)\n");
  }

  private void setupAppEngine(AppEngineStandardLibraryPanel libraryPanel, Library library) {
    CloudSdkService sdkService = mock(CloudSdkService.class);
    when(sdkService.getLibraries()).thenReturn(new File[]{});

    MavenRepositoryLibraryDownloader libraryDownloader = mock(MavenRepositoryLibraryDownloader.class);
    when(libraryDownloader.downloadLibrary(any(Module.class), any(RepositoryLibraryDescription.class),
        any(RepositoryLibraryProperties.class), anyString())).thenReturn(library);

    MutablePicoContainer applicationContainer = (MutablePicoContainer)
        ApplicationManager.getApplication().getPicoContainer();
    applicationContainer.unregisterComponent(CloudSdkService.class.getName());
    applicationContainer.registerComponentInstance(
        CloudSdkService.class.getName(), sdkService);
    applicationContainer.unregisterComponent(MavenRepositoryLibraryDownloader.class.getName());
    applicationContainer.registerComponentInstance(
        MavenRepositoryLibraryDownloader.class.getName(), libraryDownloader);

    FrameworkSupportInModuleConfigurable configurable = selectFramework(AppEngineFrameworkType.ID);
    if (libraryPanel != null && configurable instanceof AppEngineSupportConfigurable) {
      ((AppEngineSupportConfigurable) configurable).setAppEngineStandardLibraryPanel(libraryPanel);
    }
    AppEngineSupportProvider.setSdkPath(configurable, AppEngineCodeInsightTestCase.getSdkPath());
  }

  @NotNull
  private VirtualFile assertFileExist(String relativePath) {
    VirtualFile file = getContentRoot().findFileByRelativePath(relativePath);
    assertNotNull("File not found: " + relativePath, file);
    return file;
  }

  private void assertRunConfigurationCreated(Artifact artifactToDeploy) {
    List<RunConfiguration> list = RunManager.getInstance(myProject).getConfigurationsList(AppEngineServerConfigurationType.getInstance());
    CommonModel configuration = assertInstanceOf(assertOneElement(list), CommonModel.class);
    assertSameElements(configuration.getDeployedArtifacts(), artifactToDeploy);
  }
}
