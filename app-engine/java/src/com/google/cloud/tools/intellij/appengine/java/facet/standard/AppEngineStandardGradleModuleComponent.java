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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A module-level persistent component for storing information about App Engine Gradle modules. */
@State(name = AppEngineStandardGradleModuleComponent.COMPONENT_NAME)
public class AppEngineStandardGradleModuleComponent
    implements ModuleComponent, PersistentStateComponent<AppEngineStandardGradleModuleComponent> {
  static final String COMPONENT_NAME = "AppEngineStandardGradleModuleComponent";

  public String gradleBuildDir;
  public String gradleModuleDir;

  public static AppEngineStandardGradleModuleComponent getInstance(@NotNull Module module) {
    return module.getComponent(AppEngineStandardGradleModuleComponent.class);
  }

  /** Returns, optionally, the path to the Gradle build directory. */
  public Optional<String> getGradleBuildDir() {
    return Optional.ofNullable(gradleBuildDir);
  }

  /** Sets the path to the Gradle build directory. */
  public void setGradleBuildDir(@Nullable String gradleBuildDir) {
    this.gradleBuildDir = gradleBuildDir;
  }

  /** Returns, optionally, the path to the root of the Gradle module. */
  public Optional<String> getGradleModuleDir() {
    return Optional.ofNullable(gradleModuleDir);
  }

  /** Sets the path to the root of the Gradle module. */
  public void setGradleModuleDir(@Nullable String gradleModuleDir) {
    this.gradleModuleDir = gradleModuleDir;
  }

  @Nullable
  @Override
  public AppEngineStandardGradleModuleComponent getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull AppEngineStandardGradleModuleComponent state) {
    gradleBuildDir = state.gradleBuildDir;
    gradleModuleDir = state.gradleModuleDir;
  }
}
