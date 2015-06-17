package com.google.gct.idea.appengine.gradle;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalProject;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.util.containers.FactoryMap;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataService;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * GradleInvoker provides helper methods to invoke gradle tasks within IntelliJ
 */
public class GradleInvoker {
  private static final Logger LOG = Logger.getInstance(GradleInvoker.class);

  private GradleInvoker() {

  }

  /**
   * Executes a gradle task for the given module
   */
  public static void executeTask(String taskName, Module module, TaskCallback callback, ProgressExecutionMode mode) {
    String projectId = ExternalSystemApiUtil.getExternalProjectId(module);
    if (Strings.isNullOrEmpty(projectId)) {
      LOG.warn("Error retrieving module gradle id, could not execute gradle task.");
      return;
    }

    String fullTaskName = ExternalSystemApiUtil.getExternalProjectId(module) + taskName;

    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalProjectPath(ExternalSystemApiUtil.getExternalProjectPath(module));
    settings.setTaskNames(Collections.singletonList(fullTaskName));
    settings.setScriptParameters("");
    settings.setVmOptions("");
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());

    ExternalSystemUtil
        .runTask(settings, DefaultRunExecutor.EXECUTOR_ID, module.getProject(),
            GradleConstants.SYSTEM_ID, callback, mode);

  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @Nullable
  public static ExternalProject getGradleModelItem(Module module) {
    final ExternalProjectDataService externalProjectDataService =
        (ExternalProjectDataService) ServiceManager.getService(ProjectDataManager.class).getDataService(ExternalProjectDataService.KEY);

    assert externalProjectDataService != null;
    final Map<String, ExternalProject> lazyExternalProjectMap = new FactoryMap<String, ExternalProject>() {
      @Nullable
      @Override
      protected ExternalProject create(String gradleProjectPath) {
        return externalProjectDataService.getRootExternalProject(GradleConstants.SYSTEM_ID, new File(gradleProjectPath));
      }
    };

    final String gradleProjectPath = module.getOptionValue(ExternalSystemConstants.ROOT_PROJECT_PATH_KEY);
    if (Strings.isNullOrEmpty(gradleProjectPath)) {
      return null;
    }
    final ExternalProject externalRootProject = lazyExternalProjectMap.get(gradleProjectPath);
    if (externalRootProject == null) {
      return null;
    }
    return externalProjectDataService.findExternalProject(externalRootProject, module);
  }
}
