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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Application extension point that provides the option to extend list of supported runtime
 * configurations to add service account related environment variables to.
 */
public interface ServiceAccountKeyRuntimeConfigurationProvider {
  ExtensionPointName<ServiceAccountKeyRuntimeConfigurationProvider> EP_NAME =
      new ExtensionPointName<>("com.google.gct.cloudapis.serviceAccountRuntimeConfiguration");

  List<RunnerAndConfigurationSettings> getRunConfigurationsForServiceAccount(
      @NotNull Project project);

  /**
   * Adds the environment variables for the Google Cloud Libraries to the list of environment
   * variables for {@code configuration} if they don't exist. If they exist, it replaces them.
   * Configuration is guaranteed to be one from the list returned by {@link
   * #getRunConfigurationsForServiceAccount(Project)} method of this instance.
   *
   * @return Empty optional without error message in case of success, error message in case of any
   *     error.
   */
  Optional<String> addEnvironmentVariablesToConfiguration(
      RunnerAndConfigurationSettings configuration,
      Set<EnvironmentVariable> serviceAccountEnvironmentVariables);
}
