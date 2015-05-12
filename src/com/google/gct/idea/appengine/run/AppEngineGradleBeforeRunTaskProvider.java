/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.appengine.run;

import com.android.tools.idea.gradle.GradleSyncState;
import com.android.tools.idea.gradle.facet.AndroidGradleFacet;
import com.android.tools.idea.gradle.project.GradleProjectImporter;
import com.android.tools.idea.gradle.project.GradleSyncListener;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.idea.util.GctBundle;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.ThreeState;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.Icon;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task provider that creates task to sync and build App Engine Gradle projects before they're run.
 */
public class AppEngineGradleBeforeRunTaskProvider extends ExternalSystemBeforeRunTaskProvider {

  static final Key<ExternalSystemBeforeRunTask> ID = Key.create("AppEngine.Gradle.BeforeRunTask");
  static final String TASK_NAME= GctBundle.message("appengine.gradle.before.run.name");
  private static final Logger LOG = Logger.getInstance(AppEngineGradleBeforeRunTaskProvider.class);
  private final Project myProject;

  public AppEngineGradleBeforeRunTaskProvider(@NotNull Project project) {
    super(GradleConstants.SYSTEM_ID, project, ID);
    myProject = project;
  }


  @Override
  public boolean canExecuteTask(RunConfiguration configuration, ExternalSystemBeforeRunTask beforeRunTask) {
    if (configuration instanceof AppEngineRunConfiguration) {
      return super.canExecuteTask(configuration, beforeRunTask);
    }
    return false;
  }

  @NotNull
  @Override
  public Key<ExternalSystemBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public String getName() {
    return TASK_NAME;
  }

  @Override
  public String getDescription(ExternalSystemBeforeRunTask task) {
    return GctBundle.message("appengine.gradle.before.run.description");
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @Override
  public Icon getTaskIcon(ExternalSystemBeforeRunTask task) {
    return GoogleCloudToolsIcons.APP_ENGINE;
  }

  @Override
  public boolean isConfigurable() {
    return false;
  }

  @Override
  public boolean configureTask(RunConfiguration runConfiguration, ExternalSystemBeforeRunTask task) {
    return true;
  }

  @Nullable
  @Override
  public ExternalSystemBeforeRunTask createTask(RunConfiguration runConfiguration) {
    // make sure only app engine run configurations have this task and
    // enable the task so it can be a default before run task for appengine run configs
    // see BaseExecuteBeforeRunDialog#getHardcodedBeforeRunTasks
    if (runConfiguration instanceof AppEngineRunConfiguration) {
      ExternalSystemBeforeRunTask task = new ExternalSystemBeforeRunTask(ID, GradleConstants.SYSTEM_ID);
      task.setEnabled(true);
      return task;
    }
    return null;
  }

  @Override
  public boolean executeTask(DataContext context, RunConfiguration configuration, ExecutionEnvironment env, ExternalSystemBeforeRunTask task) {

    assert configuration instanceof AppEngineRunConfiguration;

    final AtomicReference<String> errorMsgRef = new AtomicReference<String>();

    // If the model needs a sync, we need to sync "synchronously" before running.
    // See: https://code.google.com/p/android/issues/detail?id=70718
    GradleSyncState syncState = GradleSyncState.getInstance(myProject);
    if (syncState.isSyncNeeded() != ThreeState.NO) {
      GradleProjectImporter.getInstance().syncProjectSynchronously(myProject, false, new GradleSyncListener.Adapter() {
        @Override
        public void syncFailed(@NotNull Project project, @NotNull String errorMessage) {
          errorMsgRef.set(errorMessage);
        }
      });
    }

    String errorMsg = errorMsgRef.get();
    if (errorMsg != null) {
      // Sync failed. There is no point on continuing, because most likely the model is either not there, or has stale information,
      // including the path of the APK.
      LOG.info(GctBundle.message("appengine.gradle.before.run.sync.failed", TASK_NAME, errorMsg));
      return false;
    }

    AppEngineRunConfiguration runConfig = (AppEngineRunConfiguration)configuration;
    Module[] modules = runConfig.getModules();

    AppEngineGradleFacet facet = AppEngineGradleFacet.getAppEngineFacetByModule(modules[0]);
    if (facet == null) {
      LOG.info(GctBundle.message("appengine.gradle.before.run.no.facet", TASK_NAME));
      return false;
    }

    AndroidGradleFacet facetForPath = AndroidGradleFacet.getInstance(modules[0]);
    if (facetForPath == null) {
      LOG.info(GctBundle.message("appengine.gradle.before.run.no.path", TASK_NAME));
      return false;
    }
    final String gradlePath = facetForPath.getConfiguration().GRADLE_PROJECT_PATH;

    // run the gradle task :<module>:assemble
    task.getTaskExecutionSettings().setTaskNames(Collections.singletonList(gradlePath + ":assemble"));
    task.getTaskExecutionSettings().setExternalProjectPath(myProject.getBasePath());

    return super.executeTask(context, configuration, env, task);
  }
}
