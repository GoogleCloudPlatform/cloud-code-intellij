/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.java.util;

import static java.util.stream.Collectors.toList;

import com.google.cloud.tools.intellij.appengine.java.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.project.AppEngineProjectService;
import com.google.common.collect.Lists;
import com.intellij.openapi.module.Module;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** App Engine utility methods. */
public class AppEngineUtil {

  public static final String APP_ENGINE_WEB_XML_NAME = "appengine-web.xml";
  public static final String APP_ENGINE_WEB_XML_ROOT_TAG_NAME = "appengine-web-app";

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  /**
   * Returns the only app engine standard artifact found for the given module or null if there
   * aren't any or more than one.
   */
  @Nullable
  public static Artifact findOneAppEngineStandardArtifact(@NotNull Module module) {
    Collection<Artifact> artifacts = ArtifactUtil.getArtifactsContainingModuleOutput(module);
    Collection<Artifact> appEngineStandardArtifacts = Lists.newArrayList();
    appEngineStandardArtifacts.addAll(
        artifacts
            .stream()
            .filter(
                artifact ->
                    AppEngineProjectService.getInstance().isAppEngineStandardArtifactType(artifact))
            .collect(toList()));

    return appEngineStandardArtifacts.size() == 1
        ? appEngineStandardArtifacts.iterator().next()
        : null;
  }

  /**
   * Checks if the given module already has either App Engine Standard or Flexible facets.
   *
   * @param module project module.
   * @return True of module has standard or flexible facets, false otherwise.
   */
  public static boolean isAnyAppEngineFacetAlreadyAdded(@NotNull Module module) {
    AppEngineStandardFacet existingStandardFacet =
        AppEngineStandardFacet.getAppEngineFacetByModule(module);
    AppEngineFlexibleFacet existingFlexibleFacet = AppEngineFlexibleFacet.getFacetByModule(module);

    return existingFlexibleFacet != null || existingStandardFacet != null;
  }
}
