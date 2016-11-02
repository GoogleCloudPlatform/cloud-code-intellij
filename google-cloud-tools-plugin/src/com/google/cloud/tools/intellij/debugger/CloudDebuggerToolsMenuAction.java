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

package com.google.cloud.tools.intellij.debugger;

import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.impl.EditConfigurationsDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Creates a shortcut to the Stackdriver debugger configuration in the tools menu.
 */
public class CloudDebuggerToolsMenuAction extends AnAction {

  public CloudDebuggerToolsMenuAction() {
    super("Stackdriver Debug...", "Stackdriver Debug...",
        GoogleCloudToolsIcons.STACKDRIVER_DEBUGGER);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();

    if (project == null) {
      return;
    }

    final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);

    Predicate<ConfigurationType> isDebugType = new Predicate<ConfigurationType>() {
      @Override
      public boolean apply(@Nullable ConfigurationType configurationType) {
        return configurationType instanceof CloudDebugConfigType;
      }
    };

    ConfigurationType type = FluentIterable.from(
        Arrays.asList(runManager.getConfigurationFactories()))
        .firstMatch(isDebugType)
        .orNull();

    final ConfigurationFactory factory = type != null ? type.getConfigurationFactories()[0] : null;

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        EditConfigurationsDialog dialog = new EditConfigurationsDialog(project, factory);

        if (dialog.showAndGet()) {
          RunnerAndConfigurationSettings settings = runManager.getSelectedConfiguration();

          if (settings != null) {
            runManager.addConfiguration(settings, false /*isShared*/);
          }
        }
      }
    });
  }
}
