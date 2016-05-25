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

package com.google.cloud.tools.intellij.appengine.util;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;

import java.util.Collections;
import java.util.Set;

/**
 * App Engine utility methods.
 */
public class AppEngineUtil {

  private static final String APP_ENGINE_STANDARD_FACET_NAME = "Google App Engine";

  private AppEngineUtil() {
    // Not designed for instantiation
  }

  /**
   * A project is an App Engine standard project if it has an artifact that has both an App Engine
   * standard facet and is an App Engine standard artifact type.
   */
  public static boolean isAppEngineStandardProject(final Project project) {
    Artifact[] artifacts = ArtifactManager.getInstance(project).getArtifacts();

    for (Artifact artifact : artifacts) {
      if (hasAppEngineStandardFacet(project, artifact)
          && isAppEngineStandardArtifactType(artifact)) {
        return true;
      }
    }

    return false;
  }

  /**
   * An artifact has an app engine standard facet if it has a module who's facet name matches that
   * of the facet configured by the App Engine legacy IJ plugin.
   */
  public static boolean hasAppEngineStandardFacet(Project project, Artifact artifact) {
    Set<Module> modules = ArtifactUtil
        .getModulesIncludedInArtifacts(Collections.singletonList(artifact), project);

    for (Module module : modules) {
      for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
        if (facet != null && APP_ENGINE_STANDARD_FACET_NAME.equals(facet.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  public static boolean isAppEngineStandardArtifactType(Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "exploded-war".equalsIgnoreCase(artifactId);
  }

  public static boolean isAppEngineFlexArtifactType(Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "jar".equalsIgnoreCase(artifactId) || "war".equals(artifactId);
  }

}
