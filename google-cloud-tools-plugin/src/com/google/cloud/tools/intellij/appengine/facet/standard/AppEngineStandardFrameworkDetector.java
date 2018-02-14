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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;
import com.intellij.facet.FacetType;
import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import org.jetbrains.annotations.NotNull;

/** @author nik */
public class AppEngineStandardFrameworkDetector
    extends FacetBasedFrameworkDetector<
        AppEngineStandardFacet, AppEngineStandardFacetConfiguration> {

  public AppEngineStandardFrameworkDetector() {
    super("appengine-java-standard");
  }

  @Override
  public void setupFacet(@NotNull AppEngineStandardFacet facet, ModifiableRootModel model) {
    AppEngineStandardWebIntegration.getInstance()
        .setupRunConfigurations(
            AppEngineUtil.findOneAppEngineStandardArtifact(facet.getModule()),
            model.getModule(),
            null /*existingConfiguration*/);

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_FACET_ADD)
        .addMetadata("source", "frameworkDetect")
        .addMetadata("env", "standard")
        .ping();
  }

  @Override
  public FacetType<AppEngineStandardFacet, AppEngineStandardFacetConfiguration> getFacetType() {
    return FacetType.findInstance(AppEngineStandardFacetType.class);
  }

  @Override
  public FrameworkType getFrameworkType() {
    return AppEngineStandardFrameworkType.getFrameworkType();
  }

  @NotNull
  @Override
  public FileType getFileType() {
    return StdFileTypes.XML;
  }

  @NotNull
  @Override
  public ElementPattern<FileContent> createSuitableFilePattern() {
    return FileContentPattern.fileContent()
        .withName(AppEngineUtil.APP_ENGINE_WEB_XML_NAME)
        .xmlWithRootTag("appengine-web-app");
  }
}
