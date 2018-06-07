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

package com.google.cloud.tools.intellij.appengine.java.gradle;

import java.io.Serializable;

/** Default implementation of {@link AppEngineGradleModel}. */
public class DefaultAppEngineGradleModel implements AppEngineGradleModel, Serializable {

  private final boolean hasAppEngineGradlePlugin;
  private final String gradleBuildDir;
  private final String gradleModuleDir;

  DefaultAppEngineGradleModel(
      boolean hasAppEngineGradlePlugin, String gradleBuildDir, String gradleModuleDir) {
    this.hasAppEngineGradlePlugin = hasAppEngineGradlePlugin;
    this.gradleBuildDir = gradleBuildDir;
    this.gradleModuleDir = gradleModuleDir;
  }

  @Override
  public boolean hasAppEngineGradlePlugin() {
    return hasAppEngineGradlePlugin;
  }

  @Override
  public String gradleBuildDir() {
    return gradleBuildDir;
  }

  @Override
  public String gradleModuleDir() {
    return gradleModuleDir;
  }
}
