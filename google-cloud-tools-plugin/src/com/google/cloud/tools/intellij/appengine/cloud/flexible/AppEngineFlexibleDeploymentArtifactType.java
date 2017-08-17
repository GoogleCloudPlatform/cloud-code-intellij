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

package com.google.cloud.tools.intellij.appengine.cloud.flexible;

import java.nio.file.Path;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the supported Java artifact types that we can deploy to App Engine flexible
 * environment.
 */
public enum AppEngineFlexibleDeploymentArtifactType {
  JAR(".jar"),
  WAR(".war"),
  // Must be last since it will match every path.
  UNKNOWN("");

  @NotNull private final String extension;

  AppEngineFlexibleDeploymentArtifactType(@NotNull String extension) {
    this.extension = extension;
  }

  /**
   * Returns the right {@code AppEngineFlexibleDeploymentArtifactType} for the given {@code
   * deployPackage}.
   */
  @NotNull
  public static AppEngineFlexibleDeploymentArtifactType typeForPath(@Nullable Path deployPackage) {
    if (deployPackage == null) {
      return UNKNOWN;
    }
    return Arrays.stream(AppEngineFlexibleDeploymentArtifactType.values())
        .filter(type -> deployPackage.toString().endsWith(type.extension))
        .findFirst()
        // Just in case, but UNKNOWN should always match the filter.
        .orElse(UNKNOWN);
  }

  @NotNull
  public String getDefaultArtifactName() {
    return extension.isEmpty() ? "" : "target" + extension;
  }
}
