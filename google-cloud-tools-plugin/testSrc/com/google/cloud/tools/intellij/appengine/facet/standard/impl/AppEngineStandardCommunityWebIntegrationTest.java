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

package com.google.cloud.tools.intellij.appengine.facet.standard.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;

public class AppEngineStandardCommunityWebIntegrationTest extends PlatformTestCase {
  private AppEngineStandardCommunityWebIntegration webIntegration;
  private Module mockModule;
  private ModifiableRootModel mockModifiableRootModel;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    webIntegration = new AppEngineStandardCommunityWebIntegration();
    mockModule = mock(Module.class);
    mockModifiableRootModel = mock(ModifiableRootModel.class);
  }

  public void testSuggestParentDirectoryForAppEngineWebXml() {
    Project project = getProject();
    VirtualFile baseDir = project.getBaseDir();
    when(mockModifiableRootModel.getContentRoots()).thenReturn(new VirtualFile[] {baseDir});

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
    VirtualFile virtualFile = (VirtualFile) resultObject;
    assertTrue(virtualFile.exists());
    assertEquals(virtualFile.getPath(), project.getBasePath() + "/WEB-INF");
  }
}
