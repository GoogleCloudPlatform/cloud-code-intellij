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

package com.google.cloud.tools.intellij;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Creates or executes a run configuration of a given type. If the run configuration already exists,
 * this action will run it, otherwise it will open the dialog to create a new one.
 */
public class CloudToolsRunConfigurationAction extends AnAction {

  private ConfigurationType configType;

  public CloudToolsRunConfigurationAction(
      @NotNull ConfigurationType configType,
      @Nullable String text,
      @Nullable String description,
      @Nullable Icon icon) {
    super(text, description, icon);

    this.configType = configType;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();

    if (project == null) {
      return;
    }

    final RunnerAndConfigurationSettings settings = RunManagerImpl.getInstanceImpl(project)
        .findConfigurationByName(configType.getDisplayName());

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (settings == null) {
          createNewConfiguration(project);
        } else {
          runExistingConfiguration(project, settings);
        }
      }
    });

  }

  private void runExistingConfiguration(Project project, RunnerAndConfigurationSettings settings) {
    ProgramRunnerUtil
        .executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
  }

  private void createNewConfiguration(Project project) {
    final RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);

    final ConfigurationFactory factory =
        configType != null ? configType.getConfigurationFactories()[0] : null;

    EditConfigurationsDialog dialog = new EditConfigurationsDialog(project, factory);

    if (dialog.showAndGet()) {
      RunnerAndConfigurationSettings settings = manager.getSelectedConfiguration();

      if (settings != null) {
        manager.addConfiguration(settings, false /*isShared*/);
      }
    }
  }

}
