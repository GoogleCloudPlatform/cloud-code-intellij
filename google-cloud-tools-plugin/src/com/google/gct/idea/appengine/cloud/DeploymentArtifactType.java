/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

/**
 * Represents the supported Java artifact types that we can deploy to App Engine (Managed VMs).
 */
public enum DeploymentArtifactType {
  UNKNOWN, JAR, WAR;

  /**
   * Returns the right {@code DeploymentArtifactType} for the given {@code deployPackage}.
   */
  @NotNull
  public static DeploymentArtifactType typeForPath(@NotNull File deployPackage) {
    if (deployPackage.getPath().endsWith(".jar")) {
      return JAR;
    } else if (deployPackage.getPath().endsWith(".war")) {
      return WAR;
    }
    return UNKNOWN;
  }


  @Override
  @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
  public String toString() {
    return "." + name().toLowerCase(Locale.US);
  }
}
