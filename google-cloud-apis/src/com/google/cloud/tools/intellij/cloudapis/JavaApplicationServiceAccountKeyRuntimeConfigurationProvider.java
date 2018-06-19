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

package com.google.cloud.tools.intellij.cloudapis;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.project.Project;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class JavaApplicationServiceAccountKeyRuntimeConfigurationProvider
    implements ServiceAccountKeyRuntimeConfigurationProvider {

  @Override
  public List<RunnerAndConfigurationSettings> getRunConfigurationsForServiceAccount(
      @NotNull Project project) {
    RunManager runManager = RunManager.getInstance(project);
    return runManager.getConfigurationSettingsList(ApplicationConfigurationType.getInstance());
  }

  @Override
  public Optional<String> addEnvironmentVariablesToConfiguration(
      RunnerAndConfigurationSettings configuration,
      Set<EnvironmentVariable> serviceAccountEnvironmentVariables) {
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, configuration);

    if (runner == null) {
      return Optional.of(
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.key.dialog.update.configuration.error",
              configuration.getName()));
    }

    if (configuration.getType() instanceof ApplicationConfigurationType) {
      RunConfiguration baseConfiguration = configuration.getConfiguration();
      // general plain java application configuration type and settings.
      if (baseConfiguration instanceof ApplicationConfiguration) {
        Map<String, String> envVariables = ((ApplicationConfiguration) baseConfiguration).getEnvs();
        // make a copy in case this is read-only and append new variables.
        Map<String, String> envVariablesCopy = new HashMap<>(envVariables);
        for (EnvironmentVariable serviceAccountEnvVariable : serviceAccountEnvironmentVariables) {
          envVariablesCopy.put(
              serviceAccountEnvVariable.getName(), serviceAccountEnvVariable.getValue());
        }
        ((ApplicationConfiguration) baseConfiguration).setEnvs(envVariablesCopy);
      }
    } else {
      // unsupported configuration type, should not happen.
      return Optional.of(
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.key.dialog.update.configuration.error",
              configuration.getName()));
    }

    return Optional.empty();
  }
}
