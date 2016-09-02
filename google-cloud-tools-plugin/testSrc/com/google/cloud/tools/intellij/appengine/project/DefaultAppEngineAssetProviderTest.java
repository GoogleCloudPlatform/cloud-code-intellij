/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.project;

import static org.junit.Assert.assertEquals;

import com.google.cloud.tools.intellij.appengine.project.DefaultAppEngineAssetProvider.AppEngineWebXmlOrdering;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * Unit tests for {@link DefaultAppEngineAssetProvider}
 */
public class DefaultAppEngineAssetProviderTest {

  @Test
  public void testAppEngineWebXmlOrdering() {
    AppEngineWebXmlOrdering order = new AppEngineWebXmlOrdering();

    VirtualFile webInf = new FakeVirtualFile(new RootVirtualFile(), "WEB-INF");

    VirtualFile webInfParentWebXml = new FakeVirtualFile(webInf, "appengine-web.xml");
    VirtualFile nonWebInParentWebXml
        = new FakeVirtualFile(new RootVirtualFile(), "appengine-web.xml");

    assertEquals(1, order.compare(nonWebInParentWebXml, webInfParentWebXml));
  }

  private static class RootVirtualFile extends StubVirtualFile {
    @NotNull
    @Override
    public String getName() {
      return "root";
    }
  }

}
