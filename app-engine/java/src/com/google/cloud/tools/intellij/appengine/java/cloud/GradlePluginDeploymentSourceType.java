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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.google.common.collect.ImmutableList;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import java.util.Collection;
import java.util.Optional;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * An app-gradle-plugin based {@link DeploymentSourceType} for App Engine deployments. Supports
 * deployments from Gradle based sources. Creates a new before-run task to build and assemble the
 * exploded-war directory.
 */
public class GradlePluginDeploymentSourceType extends BuildDeploymentSourceType {

  private static final Logger log = Logger.getInstance(GradlePluginDeploymentSourceType.class);

  private static final String SOURCE_TYPE_ID = "gradle-plugin-build-source";
  private static final String GRADLE_ASSEMBLE_TASK = "assemble";
  private static final String MODULE_NAME_ATTRIBUTE = "module_name";

  public GradlePluginDeploymentSourceType() {
    super(SOURCE_TYPE_ID);
  }

  /**
   * Creates a new Gradle assemble before-run task. The app-gradle-plugin chains an "explodeWar"
   * task to gradle build tasks to created the exploded-war directory.
   */
  @Nullable
  @Override
  protected BeforeRunTask createBuildTask(Module module) {
    GradleBeforeRunTask gradleBeforeRunTask = new GradleBeforeRunTask(
        GradleBeforeRunTaskProvider.ID, GradleConstants.SYSTEM_ID);
    ExternalSystemTaskExecutionSettings settings = gradleBeforeRunTask.getTaskExecutionSettings();

    Optional<String> gradleModuleDirOptional =
        AppEngineStandardGradleModuleComponent.getInstance(module).getGradleModuleDir();

    if (gradleModuleDirOptional.isPresent()) {
      settings.setExternalProjectPath(gradleModuleDirOptional.get());
    } else {
      log.warn(
          "Gradle module root directory not found. Unable to set root path for Gradle "
              + "assemble before run task.");
    }

    settings.setTaskNames(ImmutableList.of(GRADLE_ASSEMBLE_TASK));
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());

    return gradleBeforeRunTask;
  }

  @NotNull
  @Override
  public GradlePluginDeploymentSource load(@NotNull Element tag, @NotNull Project project) {
    GradlePluginDeploymentSource source =
        new GradlePluginDeploymentSource(
            ModulePointerManager.getInstance(project)
                .create(tag.getAttributeValue(MODULE_NAME_ATTRIBUTE)));
    source.setProjectName(tag.getAttributeValue(CLOUD_PROJECT_ATTRIBUTE));
    source.setVersion(tag.getAttributeValue(APP_ENGINE_VERSION_ATTRIBUTE));

    return source;
  }

  @Override
  public void save(@NotNull ModuleDeploymentSource moduleDeploymentSource, @NotNull Element tag) {
    tag.setAttribute(
        MODULE_NAME_ATTRIBUTE, moduleDeploymentSource.getModulePointer().getModuleName());

    super.save(moduleDeploymentSource, tag);
  }

  @NotNull
  @Override
  protected Collection<? extends BeforeRunTask> getBuildTasks(
      RunManagerEx runManager, RunConfiguration configuration) {
    return runManager.getBeforeRunTasks(configuration, GradleBeforeRunTaskProvider.ID);
  }

  @Override
  protected boolean hasBuildTaskForModule(
      Collection<? extends BeforeRunTask> beforeRunTasks, Module module) {
    return beforeRunTasks.stream().anyMatch(task -> task instanceof GradleBeforeRunTask);
  }

  /**
   * A Gradle specific {@link ExternalSystemBeforeRunTask}.
   */
  private static class GradleBeforeRunTask extends ExternalSystemBeforeRunTask {

    GradleBeforeRunTask(
        @NotNull Key<ExternalSystemBeforeRunTask> providerId,
        @NotNull ProjectSystemId systemId) {
      super(providerId, systemId);

      setEnabled(true);
    }
  }
}
