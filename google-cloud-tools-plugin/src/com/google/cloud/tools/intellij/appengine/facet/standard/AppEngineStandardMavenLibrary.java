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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.roots.DependencyScope;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang.WordUtils;

/**
 * Defines the available App Engine standard maven-sourced libraries.
 */
public enum AppEngineStandardMavenLibrary {
  SERVLET_API(
      GctBundle.message("appengine.library.servlet.api.name"),
      "javax.servlet",
      "servlet-api",
      "2.5",
      DependencyScope.PROVIDED),
  JSTL(
      GctBundle.message("appengine.library.jstl.api.name"),
      "javax.servlet",
      "jstl",
      "1.2",
      DependencyScope.PROVIDED),
  APP_ENGINE_API(
      GctBundle.message("appengine.library.app.engine.api.name"),
      "com.google.appengine",
      "appengine-api-1.0-sdk",
      AppEngineStandardMavenLibrary.RELEASE_VERSION_ID,
      DependencyScope.COMPILE),
  ENDPOINTS(
      GctBundle.message("appengine.library.endpoints.api.name"),
      "com.google.endpoints",
      "endpoints-framework",
      AppEngineStandardMavenLibrary.RELEASE_VERSION_ID,
      DependencyScope.COMPILE),
  OBJECTIFY(
      GctBundle.message("appengine.library.objectify.api.name"),
      "com.googlecode.objectify",
      "objectify",
      AppEngineStandardMavenLibrary.RELEASE_VERSION_ID,
      DependencyScope.COMPILE);

  private final String displayName;
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final DependencyScope scope;

  private static final String RELEASE_VERSION_ID = "RELEASE";

  AppEngineStandardMavenLibrary(String displayName,
      String groupId,
      String artifactId,
      String version,
      DependencyScope scope) {
    this.displayName = displayName;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.scope = scope;
  }

  public String getDisplayName() {
    return displayName;
  }

  public DependencyScope getScope() {
    return scope;
  }

  public static Optional<AppEngineStandardMavenLibrary> getLibraryByDisplayName(final String name) {
    return Arrays.stream(AppEngineStandardMavenLibrary.values())
        .filter(library -> name.equals(library.getDisplayName()))
        .findAny();
  }

  public static Optional<AppEngineStandardMavenLibrary> getLibraryByMavenDisplayName(
      final String name) {
    return Arrays.stream(AppEngineStandardMavenLibrary.values())
        .filter(library -> name.equals(library.toMavenDisplayVersion()))
        .findAny();
  }

  /**
   * Certain maven versions like "LATEST" are displayed differently - e.g. "Latest", so we need to
   * reconstruct the maven display name manually
   */
  public String toMavenDisplayVersion() {
    return groupId + ":" + artifactId + ":" + WordUtils.capitalize(version.toLowerCase());
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }
}
