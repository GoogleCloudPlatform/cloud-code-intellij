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
package com.google.cloud.tools.intellij.appengine.java;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkInternals;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.util.CommonProcessors;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.MutablePicoContainer;

/** @author nik */
public abstract class AppEngineCodeInsightTestCase extends UsefulTestCase {
  private JavaModuleFixtureBuilder myModuleBuilder;
  private IdeaProjectTestFixture myProjectFixture;
  protected CodeInsightTestFixture myCodeInsightFixture;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Fixes https://youtrack.jetbrains.com/issue/IDEA-129297. Only occurs in Jenkins.
    VfsRootAccess.allowRootAccess(System.getProperty("user.dir"));
    final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder =
        JavaTestFixtureFactory.createFixtureBuilder(getName());
    myModuleBuilder = fixtureBuilder.addModule(JavaModuleFixtureBuilder.class);
    myProjectFixture = fixtureBuilder.getFixture();
    myCodeInsightFixture = createCodeInsightFixture(getBaseDirectoryPath());
    new WriteAction() {
      @Override
      protected void run(@NotNull final Result result) {
        addAppEngineSupport(myProjectFixture.getModule());
      }
    }.execute();
  }

  protected abstract String getBaseDirectoryPath();

  private void addAppEngineSupport(Module module) {
    CloudSdkService sdkService = mock(CloudSdkService.class);
    CloudSdkInternals mockSdkInternals = mock(CloudSdkInternals.class);
    when(mockSdkInternals.getWebSchemeFile()).thenReturn(getWebSchemeFile());
    CloudSdkInternals.setInstance(mockSdkInternals);

    MutablePicoContainer applicationContainer =
        (MutablePicoContainer) ApplicationManager.getApplication().getPicoContainer();
    applicationContainer.unregisterComponent(CloudSdkService.class.getName());
    applicationContainer.registerComponentInstance(CloudSdkService.class.getName(), sdkService);

    FacetManager.getInstance(module)
        .addFacet(AppEngineStandardFacet.getFacetType(), "AppEngine", null);
  }

  public static File getWebSchemeFile() {
    return new File(getTestDataPath(), "sdk/docs/appengine-web.xsd");
  }

  public static String getSdkPath() {
    return FileUtil.toSystemIndependentName(new File(getTestDataPath(), "sdk").getAbsolutePath());
  }

  @Override
  protected void tearDown() throws Exception {
    myCodeInsightFixture.tearDown();
    super.tearDown();
  }

  protected CodeInsightTestFixture createCodeInsightFixture(final String relativeTestDataPath)
      throws Exception {
    final String testDataPath = new File(getTestDataPath(), relativeTestDataPath).getAbsolutePath();
    final CodeInsightTestFixture codeInsightFixture =
        JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(myProjectFixture);
    codeInsightFixture.setTestDataPath(testDataPath);
    final TempDirTestFixture tempDir = codeInsightFixture.getTempDirFixture();
    myModuleBuilder.addSourceContentRoot(tempDir.getTempDirPath());
    codeInsightFixture.setUp();
    final VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByPath(testDataPath);
    Assert.assertNotNull("Test data directory not found: " + testDataPath, dir);
    VfsUtil.processFilesRecursively(dir, new CommonProcessors.CollectProcessor<VirtualFile>());
    dir.refresh(false, true);
    tempDir.copyAll(
        testDataPath,
        "",
        new VirtualFileFilter() {
          @Override
          public boolean accept(VirtualFile file) {
            return !file.getName().contains("_after");
          }
        });
    return codeInsightFixture;
  }

  public static File getTestDataPath() {
    try {
      URL resource = AppEngineCodeInsightTestCase.class.getResource("/sdk");
      File testDataRoot = Paths.get(resource.toURI()).toFile().getParentFile();
      return testDataRoot;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
