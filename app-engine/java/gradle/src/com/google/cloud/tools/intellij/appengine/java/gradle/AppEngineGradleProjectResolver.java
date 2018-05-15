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

import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import java.util.Set;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension;

/**
 * A {@link GradleProjectResolverExtension} for enhancing Gradle project resolution with App Engine
 * related information. It will add the {@link AppEngineGradleModule} to the {@link
 * DataNode<ModuleData>} tree. It is called during Gradle import.
 */
@Order(ExternalSystemConstants.UNORDERED)
public class AppEngineGradleProjectResolver extends AbstractProjectResolverExtension {

  /**
   * Locates available {@link AppEngineGradleModel} to populate the gradle module with the model.
   */
  @Override
  public void populateModuleExtraModels(
      @NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    AppEngineGradleModel model =
        resolverCtx.getExtraProject(gradleModule, AppEngineGradleModel.class);

    if (model != null) {
      AppEngineGradleModule appEngineGradleModule =
          new AppEngineGradleModule(gradleModule.getName(), model);
      ideModule.createChild(
          AppEngineGradleProjectDataService.APP_ENGINE_MODEL_KEY, appEngineGradleModule);
    }

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return Sets.newHashSet(AppEngineGradleModel.class);
  }

  @NotNull
  @Override
  public Set<Class> getToolingExtensionsClasses() {
    return Sets.newHashSet(AppEngineGradleModelBuilderService.class);
  }
}
