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
package com.google.gct.idea.appengine.gradle.service;

import com.android.tools.idea.gradle.AndroidProjectKeys;
import com.android.tools.idea.gradle.GradleSyncState;
import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.appengine.gradle.project.IdeaAppEngineProject;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.RunResult;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * Add necessary configuration information to an appengine modules identified by
 * {@link com.google.gct.idea.appengine.gradle.project.AppEngineGradleProjectResolver}
 */
public class AppEngineGradleProjectDataService implements ProjectDataService<IdeaAppEngineProject, Void> {

  private static final Logger LOG = Logger.getInstance(AppEngineGradleProjectDataService.class);
  // TODO: move this somewhere else, maybe a keys file
  @NotNull public static final Key<IdeaAppEngineProject> IDE_APP_ENGINE_PROJECT =
    Key.create(IdeaAppEngineProject.class, AndroidProjectKeys.IDE_ANDROID_PROJECT.getProcessingWeight() + 10);

  @NotNull
  @Override
  public Key<IdeaAppEngineProject> getTargetDataKey() {
    return IDE_APP_ENGINE_PROJECT;
  }

  /** Add facet to App Engine module on imported modules */
  @Override
  public void importData(@NotNull final Collection<DataNode<IdeaAppEngineProject>> toImport,
                         @NotNull final Project project, boolean synchronous) {
    if (toImport.isEmpty()) {
      return;
    }

    RunResult result = new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        Map<String, IdeaAppEngineProject> importModulesMap = indexByModuleName(toImport);
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          if (importModulesMap.containsKey(module.getName())) {
            addAppEngineGradleFacet(importModulesMap.get(module.getName()), module);
          }
        }
      }
    }.execute();
    Throwable error = result.getThrowable();
    if (error != null) {
      LOG.error(String.format("Failed to set up App Engine Gradle Modules"));
      GradleSyncState.getInstance(project).syncFailed(error.getMessage());
    }
  }

  @Override
  public void removeData(@NotNull Collection<? extends Void> toRemove, @NotNull Project project, boolean synchronous) {

  }

  private static void addAppEngineGradleFacet(IdeaAppEngineProject ideaAppEngineProject, Module appEngineModule) {
    //Module does not have AppEngine-Gradle facet. Create one and add it.
    FacetManager facetManager = FacetManager.getInstance(appEngineModule);
    ModifiableFacetModel model = facetManager.createModifiableModel();
    AppEngineGradleFacet facet = AppEngineGradleFacet.getInstance(appEngineModule);
    if (facet == null) {
      try {
        facet = facetManager.createFacet(AppEngineGradleFacet.getFacetType(), AppEngineGradleFacet.NAME, null);
        model.addFacet(facet);
      }
      finally {
        model.commit();
      }
    }

    //deserialize state from ideaAppEngineProject into facet config.
    if (facet != null) {
      facet.getConfiguration().getState().APPENGINE_SDKROOT = ideaAppEngineProject.getDelegate().getAppEngineSdkRoot();
      facet.getConfiguration().getState().HTTP_ADDRESS = ideaAppEngineProject.getDelegate().getHttpAddress();
      facet.getConfiguration().getState().HTTP_PORT = ideaAppEngineProject.getDelegate().getHttpPort();
      for (String flag : ideaAppEngineProject.getDelegate().getJvmFlags()) {
        facet.getConfiguration().getState().JVM_FLAGS.add(flag);
      }
      facet.getConfiguration().getState().WAR_DIR = ideaAppEngineProject.getDelegate().getWarDir().getAbsolutePath();
      facet.getConfiguration().getState().WEB_APP_DIR = ideaAppEngineProject.getDelegate().getWebAppDir().getAbsolutePath();
    }
  }

  @NotNull
  private static Map<String, IdeaAppEngineProject> indexByModuleName(@NotNull Collection<DataNode<IdeaAppEngineProject>> dataNodes) {
    Map<String, IdeaAppEngineProject> index = Maps.newHashMap();
    for (DataNode<IdeaAppEngineProject> d : dataNodes) {
      IdeaAppEngineProject appEngineProject = d.getData();
      index.put(appEngineProject.getModuleName(), appEngineProject);
    }
    return index;
  }
}
