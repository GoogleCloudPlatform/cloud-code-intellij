/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.apis;

import com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates;
import java.util.List;
import org.jetbrains.idea.maven.dom.model.MavenDomDependency;
import org.jetbrains.idea.maven.model.MavenId;

/** Holds utility methods for working with {@link org.jetbrains.idea.maven} classes. */
public class MavenUtils {

  private MavenUtils() {}

  /**
   * Returns {@code true} if the given {@link MavenId} is in the given list of {@link
   * MavenDomDependency dependencies}.
   *
   * <p>Note that equality is tested via matching group IDs and artifact IDs. Equality of versions
   * is not required. This prevents adding duplicate dependencies for the same artifact, but with a
   * different version.
   *
   * @param mavenId the {@link MavenId} to check for existence in the given list of dependencies
   * @param dependencies the list of {@link MavenDomDependency} objects that currently exist in the
   *     DOM model
   */
  static boolean isMavenIdInDependencyList(MavenId mavenId, List<MavenDomDependency> dependencies) {
    return dependencies
        .stream()
        .anyMatch(
            dependency ->
                mavenId.equals(
                    dependency.getGroupId().getStringValue(),
                    dependency.getArtifactId().getStringValue()));
  }

  /**
   * Returns a new {@link MavenId} whose values are based on the given {@link
   * com.google.cloud.tools.libraries.json.CloudLibraryClientMavenCoordinates}.
   *
   * @param mavenCoordinates the {@link CloudLibraryClientMavenCoordinates} to convert to a {@link
   *     MavenId}
   */
  static MavenId toMavenId(CloudLibraryClientMavenCoordinates mavenCoordinates) {
    return new MavenId(
        mavenCoordinates.getGroupId(),
        mavenCoordinates.getArtifactId(),
        mavenCoordinates.getVersion());
  }
}
