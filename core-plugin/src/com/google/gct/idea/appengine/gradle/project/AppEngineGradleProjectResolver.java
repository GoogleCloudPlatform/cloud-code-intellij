/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.appengine.gradle.project;

import com.google.appengine.gradle.model.AppEngineModel;
import com.google.common.collect.Sets;
import com.google.gct.idea.appengine.gradle.service.AppEngineGradleProjectDataService;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

import org.gradle.tooling.model.idea.IdeaModule;

import java.io.File;
import java.util.Set;

/**
 * Project Resolved called during gradle import, it adds an appengine module key so the
 * {@link com.google.gct.idea.appengine.gradle.service.AppEngineGradleProjectDataService} can identify the module and add metadata
 */
@Order(ExternalSystemConstants.UNORDERED)
public class AppEngineGradleProjectResolver extends AbstractProjectResolverExtension {

  public AppEngineGradleProjectResolver() {}

  /** Identify modules that can populate the AppEngineModule gradle builder model */
  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {

    File moduleFilePath = new File(FileUtil.toSystemDependentName(ideModule.getData().getModuleFilePath()));
    File moduleRootDirPath = moduleFilePath.getParentFile();

    AppEngineModel appEngineModel = resolverCtx.getExtraProject(gradleModule, AppEngineModel.class);
    if (appEngineModel != null) {
      IdeaAppEngineProject ideaAppEngineProject =
        new IdeaAppEngineProject(gradleModule.getName(), moduleRootDirPath, appEngineModel);
      ideModule.createChild(AppEngineGradleProjectDataService.IDE_APP_ENGINE_PROJECT, ideaAppEngineProject);
    }

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  /** AppEngineModel is the only class we really care about in our resolver */
  @Override
  @NotNull
  public Set<Class> getExtraProjectModelClasses() {
    return Sets.<Class>newHashSet(AppEngineModel.class);
  }
}
