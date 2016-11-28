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

package com.google.cloud.tools.intellij.jps;

import com.google.cloud.tools.intellij.jps.model.JpsStackdriverModuleExtension;
import com.google.cloud.tools.intellij.jps.model.impl.JpsStackdriverModuleExtensionImpl;

import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsSerializationTestCase;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Facet serialization unit tests.
 */
public class JpsStackdriverSerializationTest extends JpsSerializationTestCase {

  public void testLoad() {
    loadProject("serialization/stackdriver/stackdriver.ipr");
    JpsModule module = assertOneElement(myProject.getModules());
    assertEquals("stackdriver", module.getName());
    JpsStackdriverModuleExtension extension = module.getContainer().getChild(
        JpsStackdriverModuleExtensionImpl.ROLE);
    assertNotNull(extension);
    assertEquals(extension.getCloudSdkPath(), Paths.get("Downloads/google-cloud-sdk"));
    assertEquals(extension.getModuleSourceDirectory(), Paths.get(""));
    assertEquals(extension.isGenerateSourceContext(), false);
    assertEquals(extension.isIgnoreErrors(), false);
  }

  @Override
  protected String getTestDataFileAbsolutePath(String relativePath) {
    URL resource = JpsStackdriverSerializationTest.class.getResource("/serialization");
    try {
      return Paths.get(resource.toURI()).getParent().resolve(relativePath).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
