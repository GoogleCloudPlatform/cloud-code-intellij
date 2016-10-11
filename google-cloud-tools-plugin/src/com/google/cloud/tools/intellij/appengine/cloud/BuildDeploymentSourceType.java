/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;

/**
 * Base class for build-system (e.g. maven or gradle) deployment source types. Deployment sources
 * for specific build-systems should extend this providing the build-system specific
 * implementations.
 */
public abstract class BuildDeploymentSourceType extends DeploymentSourceType<ModuleDeploymentSource> {

  public BuildDeploymentSourceType(@NotNull String id) {
    super(id);
  }

  /**
   * Creates a pre-deploy task ({@link BeforeRunTask}) for the given build-system and attaches it to
   * this module deployment source type. Invoked when a new deployment configuration is created.
   * <p/>
   * Provides the common functionality for creating the build-system packaging task, delegating
   * build-system specific functions to the concrete sub-types.
   * <p/>
   * Only creates a new task if one is not already configured.
   */
  @Override
  public void setBuildBeforeRunTask(
      @NotNull RunConfiguration configuration,
      @NotNull ModuleDeploymentSource source) {

    Module module = source.getModule();

    if (module == null) {
      return;
    }

    setConfiguration(configuration);

    RunManagerEx runManager = RunManagerEx.getInstanceEx(configuration.getProject());
    final Collection<? extends BeforeRunTask> buildTasks =
        getBuildTasks(runManager, configuration);

    if (!hasBuildTaskForModule(buildTasks, module)) {
      BeforeRunTask buildTask = createBuildTask(module);
      if (buildTask != null) {
        List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(configuration);
        tasks.add(buildTask);
        runManager.setBeforeRunTasks(configuration, tasks, true);
      }
    }
  }

  /**
   * Updates the pre-deploy build tasks ({@link BeforeRunTask}) when the deployment source is
   * updated.
   * <p/>
   * Similar to {@link BuildDeploymentSourceType#setBuildBeforeRunTask(RunConfiguration,
   * ModuleDeploymentSource)}, but it is invoked when switching between deployment sources in the
   * UI.
   * <p/>
   * Only creates a new task if one is not already configured. In following the IntelliJ pattern
   * used for bundled deployment sources, this does NOT remove any existing tasks.
   */
  @Override
  public void updateBuildBeforeRunOption(
      @NotNull JComponent runConfigurationEditorComponent,
      @NotNull Project project,
      @NotNull ModuleDeploymentSource source,
      boolean select) {

    final DataContext dataContext =
        DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor =
        ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);

    Module module = source.getModule();

    if (module == null || editor == null) {
      return;
    }

    List<BeforeRunTask> buildTasks = editor.getStepsBeforeLaunch();

    if (select && !hasBuildTaskForModule(buildTasks, module)) {
      BeforeRunTask buildTask = createBuildTask(module);
      if (buildTask != null) {
        editor.addBeforeLaunchStep(buildTask);
      }
    }
  }

  /**
   * Returns the collection of already configured {@link BeforeRunTask} subtypes for the given
   *     build-system.
   */
  @NotNull
  protected abstract Collection<? extends BeforeRunTask> getBuildTasks(
      RunManagerEx runManager,
      RunConfiguration configuration);

  /**
   * Creates a new instance of a {@link BeforeRunTask} for the corresponding build-system.
   * <p/>
   * This build task should encapsulate packaging of the build artifact for the supplied module.
   *
   * @param module for which this task is scoped
   * @return a new build task
   */

  @Nullable
  protected abstract BeforeRunTask createBuildTask(Module module);

  /**
   * Determines if there is already a configured build task in the supplied collection.
   * <p/>
   * Implementors should consider only those tasks corresponding to their build-systems, those that
   * produce the build artifact, and those scoped to the supplied module.
   *
   * @return boolean indicating if a build task exists.
   */
  protected abstract boolean hasBuildTaskForModule(
      Collection<? extends BeforeRunTask> beforeRunTasks, Module module);

  /**
   * Manually set the deployment configuration so that its available immediately in the deployment
   * configuration dialog even if the user does not trigger any UI actions. This prevents downstream
   * npe's in {@link DeployToServerRunConfiguration#checkConfiguration()}.
   */
  @SuppressWarnings("unchecked")
  private void setConfiguration(@NotNull RunConfiguration configuration) {
    if (configuration instanceof DeployToServerRunConfiguration) {
      DeployToServerRunConfiguration deployRunConfiguration =
          ((DeployToServerRunConfiguration) configuration);
      deployRunConfiguration.setDeploymentConfiguration(new AppEngineDeploymentConfiguration());
    }
  }

}
