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

package com.google.cloud.tools.intellij.appengine.facet;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import com.intellij.openapi.roots.DependencyScope;

import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;
import org.jetbrains.idea.maven.utils.library.RepositoryUtils;

import java.util.Arrays;

/**
 * Defines the available App Engine standard maven-sources libraries.
 */
public enum AppEngineStandardMavenLibrary {
  // TODO get display names from bundle (in form too)
  SERVLET_API("Servlet API", new RepositoryLibraryProperties(
      "javax.servlet", "servlet-api", "2.5"), DependencyScope.PROVIDED),
  JSTL("JSP Standard Tag Library (JSTL)", new RepositoryLibraryProperties(
      "javax.servlet", "jstl", RepositoryUtils.LatestVersionId), DependencyScope.PROVIDED),
  APP_ENGINE_API("App Engine API", new RepositoryLibraryProperties(
      "com.google.appengine", "appengine-api-1.0-sdk", RepositoryUtils.LatestVersionId),
      DependencyScope.COMPILE),
  ENDPOINTS("App Engine Endpoints", new RepositoryLibraryProperties(
      "com.google.appengine", "appengine-endpoints", RepositoryUtils.LatestVersionId),
      DependencyScope.COMPILE),
  OBJECTIFY("Objectify", new RepositoryLibraryProperties(
      "com.googlecode.objectify", "objectify", RepositoryUtils.LatestVersionId),
      DependencyScope.COMPILE);
  // TODO: do we need these?
  //  JSR_107_CACHE("JSR 107: Java Temporary Caching API", new RepositoryLibraryProperties(
  //      "net.sf.jsr107cache", "jsr107cache", RepositoryUtils.LatestVersionId)),
  //  APP_ENGINE_JSR_107_CACHE("JSR 107: Java Temporary Caching API",
  //     new RepositoryLibraryProperties("com.google.appengine", "appengine",
  //        RepositoryUtils.LatestVersionId));

  private final String displayName;
  private final RepositoryLibraryProperties libraryProperties;
  private final DependencyScope scope;

  AppEngineStandardMavenLibrary(String displayName, RepositoryLibraryProperties libraryProperties,
      DependencyScope scope) {
    this.displayName = displayName;
    this.libraryProperties = libraryProperties;
    this.scope = scope;
  }

  public String getDisplayName() {
    return displayName;
  }

  public RepositoryLibraryProperties getLibraryProperties() {
    return libraryProperties;
  }

  public DependencyScope getScope() {
    return scope;
  }

  @Nullable
  public static AppEngineStandardMavenLibrary getLibraryByDisplayName(final String name) {
    return getLibrary(new Predicate<AppEngineStandardMavenLibrary>() {
      @Override
      public boolean apply(@Nullable AppEngineStandardMavenLibrary library) {
        return library != null && name.equals(library.getDisplayName());
      }
    });
  }

  @Nullable
  public static AppEngineStandardMavenLibrary getLibraryByMavenDisplayName(final String name) {
    return getLibrary(new Predicate<AppEngineStandardMavenLibrary>() {
      @Override
      public boolean apply(@Nullable AppEngineStandardMavenLibrary library) {
        return library != null
            && name.equals(toMavenDisplayVersion(library.getLibraryProperties()));
      }
    });
  }

  public static AppEngineStandardMavenLibrary getLibrary(
      Predicate<AppEngineStandardMavenLibrary> predicate) {
    return Iterables.find(
        Arrays.asList(AppEngineStandardMavenLibrary.values()),
        predicate,
        null /*default value*/);
  }

  /**
   * Certain maven versions like "LATEST" are displayed differently - e.g. "Latest", so we need to
   * reconstruct the maven display name manually
   */
  public static String toMavenDisplayVersion(RepositoryLibraryProperties libraryProperties) {
    return libraryProperties.getGroupId() + ":"
        + libraryProperties.getArtifactId() + ":"
        + WordUtils.capitalize(libraryProperties.getVersion().toLowerCase());
  }
}
