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

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardFacet;
import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * On module import, augments App Engine Gradle modules by adding information from the {@link
 * AppEngineGradleModel} to the {@link AppEngineStandardFacet}.
 */
public class AppEngineGradleProjectDataService
    extends AbstractProjectDataService<AppEngineGradleModule, Void> {

  static final Key<AppEngineGradleModule> APP_ENGINE_MODEL_KEY =
      Key.create(AppEngineGradleModule.class, 100 /* Use a high processing weight */);

  /**
   * Sets the Gradle directory on the {@link AppEngineStandardFacet} if the module has the App
   * Engine Gradle plugin.
   */
  @Override
  public void importData(
      @NotNull Collection<DataNode<AppEngineGradleModule>> toImport,
      @Nullable ProjectData projectData,
      @NotNull Project project,
      @NotNull IdeModifiableModelsProvider modelsProvider) {
    if (toImport.isEmpty()) {
      return;
    }

    Map<String, AppEngineGradleModule> moduleNameToModel = collectByModuleName(toImport);

    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() {
        Stream.of(modelsProvider.getModules())
            .filter(module -> moduleNameToModel.containsKey(module.getName()))
            .forEach(
                module -> {
                  AppEngineGradleModule appEngineGradleModule =
                      moduleNameToModel.get(module.getName());
                  if (!PlatformUtils.isIdeaUltimate()
                      && appEngineGradleModule.getModel().hasAppEngineGradlePlugin()) {
                    AppEngineStandardGradleModuleComponent gradleModuleComponent =
                        AppEngineStandardGradleModuleComponent.getInstance(module);

                    gradleModuleComponent.setGradleBuildDir(
                        appEngineGradleModule.getModel().gradleBuildDir());
                  }
                });
      }
    }.execute();
  }

  private Map<String, AppEngineGradleModule> collectByModuleName(
      @NotNull Collection<DataNode<AppEngineGradleModule>> nodes) {
    return nodes
        .stream()
        .map(DataNode::getData)
        .collect(Collectors.toMap(AppEngineGradleModule::getModuleName, Function.identity()));
  }

  @NotNull
  @Override
  public Key<AppEngineGradleModule> getTargetDataKey() {
    return APP_ENGINE_MODEL_KEY;
  }
}
