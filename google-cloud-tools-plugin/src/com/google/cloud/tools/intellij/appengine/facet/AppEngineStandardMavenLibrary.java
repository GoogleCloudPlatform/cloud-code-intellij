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

import org.apache.commons.lang.WordUtils;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;
import org.jetbrains.idea.maven.utils.library.RepositoryUtils;

/**
 * Defines the available App Engine standard maven-sources libraries.
 */
public enum AppEngineStandardMavenLibrary {
  // TODO get display names from bundle (in form too)
  SERVLET_API("Servlet API", new RepositoryLibraryProperties(
      "javax.servlet", "servlet-api", "2.5")),
  JSTL("JSP Standard Tag Library (JSTL)", new RepositoryLibraryProperties(
      "javax.servlet", "jstl", RepositoryUtils.LatestVersionId));

  private final String displayName;
  private final RepositoryLibraryProperties libraryProperties;

  AppEngineStandardMavenLibrary(String displayName, RepositoryLibraryProperties libraryProperties) {
    this.displayName = displayName;
    this.libraryProperties = libraryProperties;
  }

  public String getDisplayName() {
    return displayName;
  }

  public RepositoryLibraryProperties getLibraryProperties() {
    return libraryProperties;
  }

  public static AppEngineStandardMavenLibrary getLibraryByDisplayName(String name) {
    for (AppEngineStandardMavenLibrary library : AppEngineStandardMavenLibrary.values()) {
      if (library.getDisplayName().equals(name)) {
        return library;
      }
    }

    return null;
  }

  /**
   * Certain maven versions like "LATEST" are displayed differently - e.g. "Latest".
   */
  public static String toDisplayVersion(String mavenVersion) {
    return WordUtils.capitalize(mavenVersion.toLowerCase());
  }
}
