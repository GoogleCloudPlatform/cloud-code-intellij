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

import static org.hamcrest.core.IsInstanceOf.any;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.facet.FacetManager;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.descriptors.ConfigFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link AppEngineStandardUltimateWebIntegration}. */
public class AppEngineStandardUltimateWebIntegrationTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private AppEngineStandardUltimateWebIntegration webIntegration;
  @Mock private Module mockModule;
  @Mock private ModifiableRootModel mockModifiableRootModel;
  @Mock private WebFacet mockWebFacet;
  @Mock private ConfigFile mockConfigFile;
  @Mock private FacetManager mockFacetManager;
  @Mock private WebRoot mockWebRoot;
  @Mock private VirtualFile mockVirtualFile1;
  @Mock private VirtualFile mockVirtualFile2;
  @Mock private VirtualFile mockVirtualFile3;
  private List<WebRoot> webRoots = new ArrayList<>();
  private String virtualFile1Name = "someName";

  @Before
  public void setUp() throws Exception {
    /**
    webIntegration = new AppEngineStandardUltimateWebIntegration();
    mockModule = mock(Module.class);
    mockModifiableRootModel = mock(ModifiableRootModel.class);
    mockWebFacet = mock(WebFacet.class);
    mockConfigFile = mock(ConfigFile.class);
    mockFacetManager = mock(FacetManager.class);
    mockWebRoot = mock(WebRoot.class);
    mockVirtualFile1 = mock(VirtualFile.class);
    mockVirtualFile2 = mock(VirtualFile.class);
    mockVirtualFile3 = mock(VirtualFile.class);**/

    webRoots.add(mockWebRoot);

    when(mockVirtualFile1.createChildDirectory(
            any(LocalFileSystem.class), AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockVirtualFile3);
    when(mockVirtualFile1.findChild(AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockVirtualFile2);
    when(mockVirtualFile1.getName()).thenReturn(virtualFile1Name);
    when(mockWebRoot.getFile()).thenReturn(mockVirtualFile1);
    when(mockWebFacet.getWebRoots()).thenReturn(webRoots);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_noWebResourceDir() throws IOException {
    webRoots.clear();

    assertNull(runSuggestParentDirectoryForAppEngineWebXml());
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_noWebInfInResourceDir()
      throws IOException {
    mockVirtualFile2 = null;

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    assertEquals(mockVirtualFile3, suggestedDirectory);
  }

  public void xtestSuggestParentDirectoryForAppEngineWebXml_withWebInfFolderAsResourceDir()
      throws IOException {
    //String webInfPath = getProject().getBasePath() + "/a/b/c/WEB-INF";
    //when(mockVirtualFile1.getPath()).thenReturn(webInfPath);
    when(mockWebRoot.getFile()).thenReturn(mockVirtualFile1);
    when(mockWebFacet.getWebRoots()).thenReturn(Collections.singletonList(mockWebRoot));
    when(mockWebFacet.getWebXmlDescriptor()).thenReturn(null);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);

    VirtualFile suggestedDirectory = runSuggestParentDirectoryForAppEngineWebXml();
    //assertEquals(webInfPath, suggestedDirectory.getPath());
  }

  public void xtestSuggestParentDirectoryForAppEngineWebXml_withWebInfFolderInResourceDir()
      throws IOException {
    final String baseDir = "";//getProject().getBaseDir().getPath();
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
    return (VirtualFile) resultObject;
  }
}
