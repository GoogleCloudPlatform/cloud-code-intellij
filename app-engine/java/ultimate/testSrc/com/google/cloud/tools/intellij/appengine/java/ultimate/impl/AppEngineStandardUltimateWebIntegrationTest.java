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
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestDirectory;
import com.intellij.facet.FacetManager;
import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
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
  @Mock private VirtualFile mockWebRootDir;
  @Mock private VirtualFile mockExistingWebInfDir;
  @Mock private VirtualFile mockNewWebInfDir;

  @TestDirectory(name = "WEB-INF")
  File testWebInf;

  private List<WebRoot> webRoots = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    webIntegration = new AppEngineStandardUltimateWebIntegration();
    webRoots.add(mockWebRoot);

    when(mockWebRootDir.getFileSystem()).thenReturn(LocalFileSystem.getInstance());
    when(mockWebRootDir.createChildDirectory(
            LocalFileSystem.getInstance(), AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockNewWebInfDir);
    when(mockWebRootDir.findChild(AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(mockExistingWebInfDir);
    when(mockWebRootDir.getName()).thenReturn("someName");
    when(mockWebRoot.getFile()).thenReturn(mockWebRootDir);
    when(mockWebFacet.getWebRoots()).thenReturn(webRoots);
    when(mockFacetManager.getFacetsByType(WebFacet.ID))
        .thenReturn(Collections.singletonList(mockWebFacet));
    when(mockModule.getComponent(FacetManager.class)).thenReturn(mockFacetManager);
  }

  @Test
  public void testSuggestParentDirectory_withNoWebRoot_returnsNull() {
    webRoots.clear();

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertThat(suggestedDirectory).isNull();
  }

  @Test
  public void testSuggestParentDirectory_withNoWebInfInWebRoot_returnsNewWebInfDir() {
    when(mockWebRootDir.findChild(AppEngineStandardUltimateWebIntegration.WEB_INF))
        .thenReturn(null);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertThat(suggestedDirectory).isEqualTo(mockNewWebInfDir);
  }

  @Test
  public void testSuggestParentDirectory_withWebInfFolderInWebRoot_returnsExistingWebInfDir() {
    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertThat(suggestedDirectory).isEqualTo(mockExistingWebInfDir);
  }

  @Test
  public void testSuggestParentDirectory_withWebInfFolderAsResourceDir_returnsWebRootDir() {
    when(mockWebRootDir.getName()).thenReturn(AppEngineStandardUltimateWebIntegration.WEB_INF);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);
    assertThat(suggestedDirectory).isEqualTo(mockWebRootDir);
  }

  @Test
  public void testSuggestParentDirectory_withInvalidWebRoot_andWebRootPath_returnsNewWebInf() {
    when(mockWebRoot.getFile()).thenReturn(null);

    String testWebInfParentDir = testWebInf.getParent();
    when(mockWebRoot.getPresentableUrl()).thenReturn(testWebInfParentDir);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);

    assertThat(
            new File(suggestedDirectory.getPath())
                .getAbsolutePath()
                .endsWith(testWebInf.getAbsolutePath()))
        .isTrue();
  }

  @Test
  public void testSuggestParentDirectory_withWebRootEndingWithWebInf_returnsWebInfDir() {
    when(mockWebRoot.getFile()).thenReturn(null);
    when(mockWebRoot.getPresentableUrl()).thenReturn(testWebInf.getAbsolutePath());

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);

    assertThat(
            new File(suggestedDirectory.getPath())
                .getAbsolutePath()
                .endsWith(testWebInf.getAbsolutePath()))
        .isTrue();
  }

  @Test
  public void testSuggestParentDirectory_withInvalidWebRoot_andNoWebRootPath_returnsNull() {
    when(mockWebRoot.getFile()).thenReturn(null);
    when(mockWebRoot.getPresentableUrl()).thenReturn(null);

    VirtualFile suggestedDirectory =
        webIntegration.suggestParentDirectoryForAppEngineWebXml(
            mockModule, mockModifiableRootModel);

    assertThat(suggestedDirectory).isNull();
  }
}
