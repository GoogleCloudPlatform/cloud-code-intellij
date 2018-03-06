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

package com.google.cloud.tools.intellij.appengine.facet.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.facet.FacetManager;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.descriptors.ConfigFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

public class AppEngineStandardUltimateWebIntegrationTest extends PlatformTestCase {
  private AppEngineStandardUltimateWebIntegration webIntegration;
  private Module mockModule;
  private ModifiableRootModel mockModifiableRootModel;
  private WebFacet mockWebFacet;
  private ConfigFile mockConfigFile;
  private VirtualFile mockWebXmlDir;
  private VirtualFile mockWebXml;
  private FacetManager mockFacetManager;
  private WebRoot mockWebRoot;
  private VirtualFile mockVirtualFile1;
  private VirtualFile mockVirtualFile2;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    webIntegration = new AppEngineStandardUltimateWebIntegration();
    mockModule = mock(Module.class);
    mockModifiableRootModel = mock(ModifiableRootModel.class);
    mockWebFacet = mock(WebFacet.class);
    mockConfigFile = mock(ConfigFile.class);
    mockWebXmlDir = mock(VirtualFile.class);
    mockWebXml = mock(VirtualFile.class);
    mockFacetManager = mock(FacetManager.class);
    mockWebRoot = mock(WebRoot.class);
    mockVirtualFile1 = mock(VirtualFile.class);
    mockVirtualFile2 = mock(VirtualFile.class);
  }

  public void testGetDefaultAppEngineWebXmlPath() {
    assertEquals(webIntegration.getDefaultAppEngineWebXmlPath(), "src/main/webapp/WEB-INF");
  }

  public void testSuggestParentDirectoryForAppEngineWebXml_withWebXml() throws IOException {
    when(mockWebXml.getParent()).thenReturn(mockWebXmlDir);
    when(mockConfigFile.getVirtualFile()).thenReturn(mockWebXml);
    when(mockWebFacet.getWebXmlDescriptor()).thenReturn(mockConfigFile);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    assertEquals(mockWebXmlDir, suggestedDirectory);
  }

  public void testSuggestParentDirectoryForAppEngineWebXml_noWebXml_noWebInfFolder()
      throws IOException {
    VirtualFile baseDir = getProject().getBaseDir();
    when(mockModifiableRootModel.getContentRoots()).thenReturn(new VirtualFile[] {baseDir});
    when(mockWebFacet.getWebXmlDescriptor()).thenReturn(null);
    when(mockWebFacet.getWebRoots()).thenReturn(new ArrayList<>());
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    assertTrue(suggestedDirectory.exists());
    assertEquals(baseDir.getPath() + "/src/main/webapp/WEB-INF", suggestedDirectory.getPath());
  }

  public void testSuggestParentDirectoryForAppEngineWebXml_noWebXml_withWebInfFolderAsResourceDir()
      throws IOException {
    String webInfPath = getProject().getBasePath() + "/a/b/c/WEB-INF";
    when(mockVirtualFile1.getPath()).thenReturn(webInfPath);
    when(mockWebRoot.getFile()).thenReturn(mockVirtualFile1);
    when(mockWebFacet.getWebRoots()).thenReturn(Collections.singletonList(mockWebRoot));
    when(mockWebFacet.getWebXmlDescriptor()).thenReturn(null);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    assertEquals(webInfPath, suggestedDirectory.getPath());
  }

  public void testSuggestParentDirectoryForAppEngineWebXml_noWebXml_withWebInfFolderInResourceDir()
      throws IOException {
    final String baseDir = getProject().getBaseDir().getPath();
    when(mockVirtualFile1.findChild("WEB-INF")).thenReturn(mockVirtualFile2);
    when(mockVirtualFile1.getPath()).thenReturn(baseDir + "/a/b/c");
    when(mockWebRoot.getFile()).thenReturn(mockVirtualFile1);
    when(mockWebFacet.getWebRoots()).thenReturn(Collections.singletonList(mockWebRoot));
    when(mockWebFacet.getWebXmlDescriptor()).thenReturn(null);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    assertEquals(mockVirtualFile2, suggestedDirectory);
  }

  private VirtualFile runSuggestParentDirectoryForAppEngineWebXml() {
    Object resultObject =
        new WriteAction() {
          @Override
          protected void run(@NotNull Result result) throws Throwable {
            result.setResult(
                webIntegration.suggestParentDirectoryForAppEngineWebXml(
                    mockModule, mockModifiableRootModel));
          }
        }.execute().getResultObject();
    assertTrue(resultObject instanceof VirtualFile);
    return (VirtualFile) resultObject;
  }
}
