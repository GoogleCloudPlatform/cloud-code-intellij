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

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/** A {@link ModelBuilderService} for populating {@link AppEngineGradleModel}. */
public class AppEngineGradleModelBuilderService implements ModelBuilderService {

  private static final String APP_GRADLE_PLUGIN_ID = "com.google.cloud.tools.appengine";

  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(AppEngineGradleModel.class.getName());
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    boolean hasAppEngineGradlePlugin = project.getPlugins().hasPlugin(APP_GRADLE_PLUGIN_ID);

    return new DefaultAppEngineGradleModel(
        hasAppEngineGradlePlugin,
        project.getBuildDir().getAbsolutePath(),
        project.getBuildFile().getParent());
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(
      @NotNull Project project, @NotNull Exception ex) {
    return ErrorMessageBuilder.create(project, ex, "App Engine Gradle model import errors")
        .withDescription("Failed to build the App Engine Gradle model");
  }
}
