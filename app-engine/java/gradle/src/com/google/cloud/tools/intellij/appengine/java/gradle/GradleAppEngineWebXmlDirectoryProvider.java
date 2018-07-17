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

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.BuildSystemAppEngineWebXmlDirectoryProvider;
import com.google.cloud.tools.intellij.appengine.java.gradle.project.GradleProjectService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Gradle build-system aware strategy for providing the path to the appengine-web.xml directory.
 *
 * <p>Queries the {@link AppEngineStandardGradleModuleComponent} for the path to the webapp
 * directory. If not present, then returns the canonical default for Gradle.
 */
public class GradleAppEngineWebXmlDirectoryProvider
    implements BuildSystemAppEngineWebXmlDirectoryProvider {

  private static final String GRADLE_WEBAPP_DEFAULT_DIR = "src/main/webapp";

  @Override
  public Optional<String> getAppEngineWebXmlDirectoryPath(@NotNull Module module) {
    if (!GradleProjectService.getInstance().isGradleModule(module)) {
      return Optional.empty();
    }

    AppEngineStandardGradleModuleComponent gradleModuleComponent =
        AppEngineStandardGradleModuleComponent.getInstance(module);
    String gradleWebAppDir = gradleModuleComponent.getWebAppDir().orElse(null);

    if (gradleWebAppDir == null) {
      gradleWebAppDir = GRADLE_WEBAPP_DEFAULT_DIR;
    }

    if (!FileUtil.isAbsolute(gradleWebAppDir)
        && gradleModuleComponent.getGradleModuleDir().isPresent()) {
      gradleWebAppDir = gradleModuleComponent.gradleModuleDir + '/' + gradleWebAppDir;
    }

    return Optional.of(gradleWebAppDir + "/WEB-INF");
  }
}
