/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.util;

import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;

import com.intellij.openapi.module.Module;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.ArtifactManagerImpl;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.packaging.impl.elements.ArchivePackagingElement;
import com.intellij.testFramework.PlatformTestCase;

import java.util.Optional;

/** Tests for {@link AppEngineUtil}. */
public class AppEngineUtilTest extends PlatformTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testMissingAppEngineStandardFacet() {
    Optional<AppEngineStandardFacet> facet =
        AppEngineUtil.findAppEngineStandardFacet(getProject(), createArtifactWithModule());
    assertFalse(facet.isPresent());
  }

  private Artifact createArtifactWithModule() {
    Module module = createModule("test");
    ArtifactManager artifactManager = ArtifactManagerImpl.getInstance(getProject());
    ModifiableArtifactModel modifiableArtifactModel = artifactManager.createModifiableModel();
    ArchivePackagingElement archivePackagingElement =
        new ArchivePackagingElement(module.getName() + ".jar");

    final PackagingElement<?> moduleOutput =
        PackagingElementFactory.getInstance().createModuleOutput(module);
    archivePackagingElement.addFirstChild(moduleOutput);
    final ModifiableArtifact artifact =
        modifiableArtifactModel.addArtifact(
            "Project Artifacts", JarArtifactType.getInstance(), archivePackagingElement);
    artifact.setBuildOnMake(true);
    return artifact;
  }
}
