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

import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.common.collect.ImmutableList;
import com.intellij.facet.FacetType;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.ObjectPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileContent;
import java.util.Scanner;
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
        .trackEvent(GctTracking.APP_ENGINE_FACET_ADD)
        .addMetadata("source", "frameworkDetect")
        .addMetadata("env", "flex")
        .ping();
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
    return new AppEngineFlexPattern().withAppEngineFlexYamlContent();
  }

  /**
   * IntelliJ API pattern class that checks for App Engine Flex project file presence and checks it
   * has required configuration lines to avoid spurious detection.
   */
  private static final class AppEngineFlexPattern
      extends ObjectPattern<FileContent, AppEngineFlexPattern> {
    private static final ImmutableList<String> APP_ENGINE_FLEX_CONFIG_FILES =
        ImmutableList.of("app.yaml", "app.yml");

    private static final String APP_ENGINE_FLEX_REQUIRED_YAML_CONTENT = "runtime:";

    private AppEngineFlexPattern() {
      super(FileContent.class);
    }

    private AppEngineFlexPattern withAppEngineFlexYamlContent() {
      return with(
          new PatternCondition<FileContent>("with-appengine-java-flexible") {
            @Override
            public boolean accepts(@NotNull FileContent fileContent, ProcessingContext context) {
              // checks for flex engine file names and then checks for required configuration line
              // inside.
              boolean nameMatch = APP_ENGINE_FLEX_CONFIG_FILES.contains(fileContent.getFileName());
              if (nameMatch) {
                Scanner scanner = new Scanner(fileContent.getContentAsText().toString());
                while (scanner.hasNextLine()) {
                  if (scanner.nextLine().startsWith(APP_ENGINE_FLEX_REQUIRED_YAML_CONTENT)) {
                    return true;
                  }
                }
              }
              return false;
            }
          });
    }
  }
}
