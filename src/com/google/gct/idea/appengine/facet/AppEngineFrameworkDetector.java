/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.google.gct.idea.appengine.facet;

import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

/**
 * Detector to automatically find gradle appengine projects (currently disabled in plugin.xml)
 */
public class AppEngineFrameworkDetector extends FacetBasedFrameworkDetector<AppEngineGradleFacet,  AppEngineGradleFacetConfiguration> {
  public AppEngineFrameworkDetector() {
    super(AppEngineGradleFacet.ID);
  }

  @Override
  public FacetType<AppEngineGradleFacet, AppEngineGradleFacetConfiguration> getFacetType() {
    return FacetType.findInstance(AppEngineGradleFacetType.class);
  }

  @Override
  public void setupFacet(@NotNull AppEngineGradleFacet facet, ModifiableRootModel model) {
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return StdFileTypes.XML;
  }

  @NotNull
  @Override
  public ElementPattern<FileContent> createSuitableFilePattern() {
    return FileContentPattern.fileContent().withName("appengine-web.xml").xmlWithRootTag("appengine-web-app");
  }
}
