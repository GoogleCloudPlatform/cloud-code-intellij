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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.tools.intellij.appengine.project.DefaultAppEngineAssetProvider.AppEngineWebXmlOrdering;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link DefaultAppEngineAssetProvider}
 */
public class DefaultAppEngineAssetProviderTest {

  private DefaultAppEngineAssetProvider assetProvider;

  @Before
  public void setup() {
    assetProvider = new DefaultAppEngineAssetProvider();
  }

  @Test
  public void testAppEngineWebXmlOrdering() {
    AppEngineWebXmlOrdering order = new AppEngineWebXmlOrdering();
    assertEquals(1, order.compare(createRootWebXml(), createWebInfParentWebXml()));
  }

  @Test
  public void findHighestPriorityAppEngineWebXml_allNull() {
    List<VirtualFile> appEngineWebXmls = Arrays.asList(null, null, null);
    VirtualFile result = assetProvider.findHighestPriorityAppEngineWebXml(appEngineWebXmls);
    assertNull(result);
  }

  @Test
  public void findHighestPriorityAppEngineWebXml_someNull() {
    VirtualFile expected = createWebInfParentWebXml();
    List<VirtualFile> appEngineWebXmls = Arrays.asList(null, null, createRootWebXml(), expected);
    VirtualFile result = assetProvider.findHighestPriorityAppEngineWebXml(appEngineWebXmls);
    assertTrue(expected == result);
  }

  @Test
  public void findHighestPriorityAppEngineWebXml_noNulls() {
    VirtualFile expected = createWebInfParentWebXml();
    List<VirtualFile> appEngineWebXmls = Arrays.asList(createRootWebXml(), expected);
    VirtualFile result = assetProvider.findHighestPriorityAppEngineWebXml(appEngineWebXmls);
    assertTrue(expected == result);
  }

  private VirtualFile createWebInfParentWebXml() {
    VirtualFile webInf = new FakeVirtualFile(new RootVirtualFile(), "WEB-INF");
    return new FakeVirtualFile(webInf, "appengine-web.xml");
  }

  private VirtualFile createRootWebXml() {
    return new FakeVirtualFile(new RootVirtualFile(), "appengine-web.xml");
  }

  private static class RootVirtualFile extends StubVirtualFile {
    @NotNull
    @Override
    public String getName() {
      return "root";
    }

    @Override
    public String getPath() {
      return "/";
    }
  }

}
