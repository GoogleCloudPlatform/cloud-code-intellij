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

package com.google.cloud.tools.intellij.appengine.java.ultimate.cloudapis;

import com.google.cloud.tools.intellij.appengine.java.ultimate.server.run.AppEngineLocalServerUltimateConfigurationType;
import com.google.cloud.tools.intellij.cloudapis.GoogleCloudApisMessageBundle;
import com.google.cloud.tools.intellij.cloudapis.ServiceAccountKeyRunConfigurationProvider;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.javaee.run.configuration.RunnerSpecificLocalConfigurationBit;
import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

/**
 * Extension point based on {@link ServiceAccountKeyRunConfigurationProvider}, extending list of
 * runtime targets where service key account environment variables can be added with all available
 * `App Engine Local Server Run` configurations available in IDEA Ultimate only.
 */
public class AppEngineLocalRunServiceAccountKeyRunConfigurationProvider
    implements ServiceAccountKeyRunConfigurationProvider {

  @Override
  public List<RunnerAndConfigurationSettings> getRunConfigurationsForServiceAccount(
      @NotNull Project project) {
    return RunManager.getInstance(project)
        .getConfigurationSettingsList(AppEngineLocalServerUltimateConfigurationType.getInstance());
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

    RunnerSpecificLocalConfigurationBit appEngineLocalRunSettings =
        (RunnerSpecificLocalConfigurationBit) configuration.getConfigurationSettings(runner);
    if (appEngineLocalRunSettings == null) {
      return Optional.of(
          GoogleCloudApisMessageBundle.message(
              "cloud.apis.service.account.key.dialog.update.configuration.error",
              configuration.getName()));
    }

    List<EnvironmentVariable> configurationSettingsEnvVariables =
        appEngineLocalRunSettings.getEnvVariables();

    serviceAccountEnvironmentVariables.forEach(
        serviceAccountEnvVar -> {
          OptionalInt indexOpt =
              IntStream.range(0, configurationSettingsEnvVariables.size())
                  .filter(
                      index ->
                          serviceAccountEnvVar
                              .getName()
                              .equals(configurationSettingsEnvVariables.get(index).getName()))
                  .findFirst();
          if (indexOpt.isPresent()) {
            configurationSettingsEnvVariables.set(indexOpt.getAsInt(), serviceAccountEnvVar);
          } else {
            configurationSettingsEnvVariables.add(serviceAccountEnvVar);
          }
        });
    appEngineLocalRunSettings.setEnvironmentVariables(configurationSettingsEnvVariables);

    return Optional.empty();
  }
}
