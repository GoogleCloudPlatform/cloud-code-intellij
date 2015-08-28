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

import static com.intellij.openapi.ui.MessageType.ERROR;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;

import com.google.common.collect.Maps;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.appengine.gradle.project.IdeaAppEngineProject;

import com.google.gct.idea.appengine.run.AppEngineRunConfiguration;
import com.google.gct.idea.appengine.run.AppEngineRunConfigurationType;
import com.google.gct.login.stats.UsageTrackerService;
import com.google.gct.idea.util.GctTracking;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.application.RunResult;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;

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
    Key.create(IdeaAppEngineProject.class, ProjectKeys.PROJECT.getProcessingWeight() + 25);

  private NotificationGroup myLoggingNotification;

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
            AppEngineGradleFacet facet = addAppEngineGradleFacet(importModulesMap.get(module.getName()), module);
            addAppEngineRunConfiguration(module, facet);
            UsageTrackerService.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.GRADLE_IMPORT, null, null);
          }
        }
      }
    }.execute();
    Throwable error = result.getThrowable();
    if (error != null) {
      LOG.warn("Failed to set up App Engine Gradle Modules");
      syncFailed(project, error.getMessage());
    }
  }

  //TODO notify the GradleSyncState that this has failed.
  public void syncFailed(@NotNull final Project project, @NotNull final String message) {
    String logMsg = "Gradle sync failed";
    if (isNotEmpty(message)) {
      logMsg += String.format(": %1$s", message);
    }
    addToEventLog(project, logMsg, ERROR);
  }

  private void addToEventLog( @NotNull final Project project, @NotNull String message, @NotNull MessageType type) {
    if (myLoggingNotification == null) {
      // In android studio, this group already exists, so use it if we can
      NotificationGroup registeredGroup = NotificationGroup.findRegisteredGroup("Gradle sync");
      myLoggingNotification = registeredGroup != null ? registeredGroup : NotificationGroup.logOnlyGroup("Gradle sync");
    }
    myLoggingNotification.createNotification(message, type).notify(project);
  }

  @Override
  public void removeData(@NotNull Collection<? extends Void> toRemove, @NotNull Project project, boolean synchronous) {

  }

  @NotNull
  private static AppEngineGradleFacet addAppEngineGradleFacet(IdeaAppEngineProject ideaAppEngineProject, Module appEngineModule) {
    FacetManager facetManager = FacetManager.getInstance(appEngineModule);
    ModifiableFacetModel model = facetManager.createModifiableModel();
    AppEngineGradleFacet facet = AppEngineGradleFacet.getInstance(appEngineModule);
    if (facet == null) {
      //Module does not have AppEngine-Gradle facet. Create one and add it.
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
      facet.getConfiguration().getState().JVM_FLAGS.clear();
      for (String flag : ideaAppEngineProject.getDelegate().getJvmFlags()) {
        facet.getConfiguration().getState().JVM_FLAGS.add(flag);
      }
      facet.getConfiguration().getState().WAR_DIR = ideaAppEngineProject.getDelegate().getWarDir().getAbsolutePath();
      facet.getConfiguration().getState().WEB_APP_DIR = ideaAppEngineProject.getDelegate().getWebAppDir().getAbsolutePath();
      facet.getConfiguration().getState().DISABLE_UPDATE_CHECK = ideaAppEngineProject.getDelegate().isDisableUpdateCheck();
    }
    return facet;
  }

  private static void addAppEngineRunConfiguration(@NotNull Module appEngineModule, @NotNull AppEngineGradleFacet facet) {
    final RunManagerEx runManager = RunManagerEx.getInstanceEx(appEngineModule.getProject());
    for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
      if (configuration.getName().equals(appEngineModule.getName())) {
        // TODO, we might want to check if this module already has a run configuration configured instead of the name
        return;
      }
    }
    final RunnerAndConfigurationSettings settings = runManager.
      createRunConfiguration(appEngineModule.getName(), AppEngineRunConfigurationType.getInstance().getFactory());
    settings.setSingleton(true);
    final AppEngineRunConfiguration configuration = (AppEngineRunConfiguration)settings.getConfiguration();
    configuration.setModule(appEngineModule);
    // pull configuration out of gradle
    configuration.setSyncWithGradle(true);
    runManager.addConfiguration(settings, false);
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
