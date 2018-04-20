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

package com.google.cloud.tools.intellij.appengine.java.ultimate.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.intellij.facet.FacetManager;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  @Mock private FacetManager mockFacetManager;
  @Mock private WebRoot mockWebRoot;
  @Mock private VirtualFile mockVirtualFile1;
  @Mock private VirtualFile mockVirtualFile2;
  @Mock private VirtualFile mockVirtualFile3;
  private List<WebRoot> webRoots = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    webIntegration = new AppEngineStandardUltimateWebIntegration();
    webRoots.add(mockWebRoot);

    when(mockVirtualFile1.createChildDirectory(
            LocalFileSystem.getInstance(), AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockVirtualFile3);
    when(mockVirtualFile1.findChild(AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockVirtualFile2);
    when(mockVirtualFile1.getName()).thenReturn("someName");
    when(mockWebRoot.getFile()).thenReturn(mockVirtualFile1);
    when(mockWebFacet.getWebRoots()).thenReturn(webRoots);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_noWebResourceDir() {
    webRoots.clear();

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertNull(suggestedDirectory);
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_noWebInfFolderInResourceDir() {
    when(mockVirtualFile1.findChild(AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(null);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertEquals(mockVirtualFile3, suggestedDirectory);
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_withWebInfFolderInResourceDir() {
    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertEquals(mockVirtualFile2, suggestedDirectory);
  }

  @Test
  public void testSuggestParentDirectoryForAppEngineWebXml_withWebInfFolderAsResourceDir() {
    when(mockVirtualFile1.getName()).thenReturn(AppEngineStandardUltimateWebIntegration.WEB_INF);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertEquals(mockVirtualFile1, suggestedDirectory);
  }

  @Test
  public void malformedWebRoot_noFile_suggestsNullDirectory() {
    when(mockWebRoot.getFile()).thenReturn(null);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);

    assertThat(suggestedDirectory).isNull();
  }
}
