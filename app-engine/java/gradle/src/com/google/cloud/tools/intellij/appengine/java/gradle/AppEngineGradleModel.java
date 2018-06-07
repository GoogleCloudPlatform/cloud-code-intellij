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

/** Model containing information obtained from the Gradle integration. */
public interface AppEngineGradleModel {

  /**
   * Returns {@code true} if the module has the app-gradle-plugin configured, and {@code false}
   * otherwise.
   */
  boolean hasAppEngineGradlePlugin();

  /**
   * Returns the path to the Gradle build directory where the Gradle build output and artifacts
   * live.
   */
  String gradleBuildDir();

  /** Returns the path to the root of the Gradle module */
  String gradleModuleDir();
}
