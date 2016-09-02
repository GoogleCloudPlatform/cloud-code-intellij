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

package com.google.cloud.tools.intellij.appengine.project;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.xml.XmlFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Service to retrieve assets used by Google App Engine.
 */
public abstract class AppEngineAssetProvider {

  public static AppEngineAssetProvider getInstance() {
    return ServiceManager.getService(AppEngineAssetProvider.class);
  }

  /**
   * Returns the project's appengine-web.xml or null if it does not exist. If there are multiple
   * appengine-web.xml's then it will return one of them with preference given to those under the
   * WEB-INF directory.
   */
  @Nullable
  public abstract XmlFile loadAppEngineStandardWebXml(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * @see AppEngineAssetProvider#loadAppEngineStandardWebXml(Project, Artifact). Loads the
   *     configuration at the module level.
   */
  @Nullable
  public abstract XmlFile loadAppEngineStandardWebXml(@NotNull Project project,
      @NotNull Collection<Module> module);
}
