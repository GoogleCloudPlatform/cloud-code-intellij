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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;

import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.util.indexing.FileContent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/** Detects App Engine Flexible framework in a project. */
public class AppEngineFlexibleFrameworkDetector
    extends FacetBasedFrameworkDetector<
        AppEngineFlexibleFacet, AppEngineFlexibleFacetConfiguration> {

  public AppEngineFlexibleFrameworkDetector() {
    super("appengine-java-flexible");
  }

  @Override
  public void setupFacet(@NotNull AppEngineFlexibleFacet facet, ModifiableRootModel model) {
    AppEngineFlexibleSupportProvider.addSupport(facet, model, false /* generateConfigFiles */);

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_ADD_FLEX_FACET)
        .addMetadata(GctTracking.METADATA_LABEL_KEY, "frameworkDetect")
        .ping();
    // TODO: remove
    System.out.print("Flex facet add on frameworkDetect");
  }

  @NotNull
  @Override
  public FacetType<AppEngineFlexibleFacet, AppEngineFlexibleFacetConfiguration> getFacetType() {
    return FacetType.findInstance(AppEngineFlexibleFacetType.class);
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return YAMLFileType.YML;
  }

  @NotNull
  @Override
  public ElementPattern<FileContent> createSuitableFilePattern() {
    return StandardPatterns.or(
        FileContentPattern.fileContent().withName("app.yaml"),
        FileContentPattern.fileContent().withName("app.yml"));
  }
}
