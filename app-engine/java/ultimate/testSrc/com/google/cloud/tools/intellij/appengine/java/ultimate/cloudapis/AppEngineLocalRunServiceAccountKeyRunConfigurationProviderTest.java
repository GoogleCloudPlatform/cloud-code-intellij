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

import static com.google.cloud.tools.intellij.cloudapis.ServiceAccountKeyDownloadedPanel.CLOUD_PROJECT_ENV_VAR_KEY;
import static com.google.cloud.tools.intellij.cloudapis.ServiceAccountKeyDownloadedPanel.CREDENTIAL_ENV_VAR_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.gradle.internal.impldep.org.testng.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.JavaPatchableProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.javaee.run.configuration.CommonStrategy;
import com.intellij.javaee.run.configuration.JavaeeRunConfigurationCommonSettingsBean;
import com.intellij.javaee.run.configuration.RunnerSpecificLocalConfigurationBit;
import com.intellij.javaee.run.localRun.ScriptHelper;
import com.intellij.openapi.project.Project;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineLocalRunServiceAccountKeyRunConfigurationProvider} */
public class AppEngineLocalRunServiceAccountKeyRunConfigurationProviderTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private Project mockProject;
  @Mock @TestService private RunnerRegistry mockRunnerRegistry;
  @Mock private RunnerAndConfigurationSettings mockRunnerAndConfigurationSettings;
  @Mock private JavaPatchableProgramRunner mockProgramRunner;
  @Mock private CommonStrategy mockCommonStrategy;
  @Mock private ScriptHelper mockScriptHelper;
  private RunnerSpecificLocalConfigurationBit runnerSpecificLocalConfigurationBit;

  private AppEngineLocalRunServiceAccountKeyRunConfigurationProvider appEngineConfigProvider;

  @Before
  public void setUp() {
    appEngineConfigProvider = spy(new AppEngineLocalRunServiceAccountKeyRunConfigurationProvider());

    TestConfigurationInfoProvider configurationInfoProvider = new TestConfigurationInfoProvider();
    when(mockCommonStrategy.createStartupHelper(configurationInfoProvider))
        .thenReturn(mockScriptHelper);
    when(mockCommonStrategy.createShutdownHelper(configurationInfoProvider))
        .thenReturn(mockScriptHelper);
    when(mockCommonStrategy.getSettingsBean())
        .thenReturn(new JavaeeRunConfigurationCommonSettingsBean());

    runnerSpecificLocalConfigurationBit =
        new RunnerSpecificLocalConfigurationBit(new TestConfigurationInfoProvider());
    runnerSpecificLocalConfigurationBit.setEnvironmentVariables(new ArrayList<>());

    when(mockRunnerAndConfigurationSettings.getConfigurationSettings(mockProgramRunner))
        .thenReturn(runnerSpecificLocalConfigurationBit);
    when(mockRunnerRegistry.getRunner(any(), any())).thenReturn(mockProgramRunner);

    doReturn(Collections.singletonList(mockRunnerAndConfigurationSettings))
        .when(appEngineConfigProvider)
        .getRunConfigurationsForServiceAccount(mockProject);
  }

  @Test
  public void addEnvironmentVariablesToConfiguration_whenEnvVarsDoNotExistInConfig_add() {
    String gcpProjectId = "gcpProjectId";
    String serviceAccountKeyDownloadPath = "downloadPath";
    Optional<String> errorMessage =
        appEngineConfigProvider.addEnvironmentVariablesToConfiguration(
            mockRunnerAndConfigurationSettings,
            getServiceAccountEnvironmentVariables(gcpProjectId, serviceAccountKeyDownloadPath));

    assertFalse(errorMessage.isPresent());
    List<EnvironmentVariable> actualEnvVars = runnerSpecificLocalConfigurationBit.getEnvVariables();
    assertNotNull(actualEnvVars);
    assertEquals(2, actualEnvVars.size());
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars, new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", gcpProjectId, false)));
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars,
            new EnvironmentVariable(
                "GOOGLE_APPLICATION_CREDENTIALS", serviceAccountKeyDownloadPath, false)));
  }

  @Test
  public void addEnvironmentVariablesToConfiguration_whenEnvVarsExistInConfig_update() {
    List<EnvironmentVariable> oldEnvVariables = new ArrayList<>();
    oldEnvVariables.add(new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", "oldCloudProject", false));
    oldEnvVariables.add(
        new EnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "oldCredentialPath", false));
    oldEnvVariables.add(new EnvironmentVariable("fakeName", "fakeValue", false));
    runnerSpecificLocalConfigurationBit.setEnvironmentVariables(oldEnvVariables);

    String gcpProjectId = "gcpProjectId";
    String serviceAccountKeyDownloadPath = "downloadPath";
    Optional<String> errorMessage =
        appEngineConfigProvider.addEnvironmentVariablesToConfiguration(
            mockRunnerAndConfigurationSettings,
            getServiceAccountEnvironmentVariables(gcpProjectId, serviceAccountKeyDownloadPath));

    assertFalse(errorMessage.isPresent());
    List<EnvironmentVariable> actualEnvVars = runnerSpecificLocalConfigurationBit.getEnvVariables();
    assertNotNull(actualEnvVars);
    assertEquals(3, actualEnvVars.size());
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars, new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", gcpProjectId, false)));
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars,
            new EnvironmentVariable(
                "GOOGLE_APPLICATION_CREDENTIALS", serviceAccountKeyDownloadPath, false)));
  }

  private boolean containsEnvironmentVariable(
      final List<EnvironmentVariable> environmentVariableList,
      final EnvironmentVariable environmentVariable) {
    return environmentVariableList
        .stream()
        .anyMatch(
            envVar ->
                (envVar.getName().equals(environmentVariable.getName())
                    && envVar.getValue().equals(environmentVariable.getValue())));
  }

  private Set<EnvironmentVariable> getServiceAccountEnvironmentVariables(
      String gcpProjectId, String downloadPath) {
    Set<EnvironmentVariable> environmentVariables = new HashSet<>();
    environmentVariables.add(
        new EnvironmentVariable(CLOUD_PROJECT_ENV_VAR_KEY, gcpProjectId, false));
    environmentVariables.add(new EnvironmentVariable(CREDENTIAL_ENV_VAR_KEY, downloadPath, false));
    return environmentVariables;
  }

  private class TestConfigurationInfoProvider implements ConfigurationInfoProvider {
    @NotNull
    @Override
    public ProgramRunner<?> getRunner() {
      return mockProgramRunner;
    }

    @NotNull
    @Override
    public RunConfiguration getConfiguration() {
      return mockCommonStrategy;
    }

    @Nullable
    @Override
    public RunnerSettings getRunnerSettings() {
      return new GenericDebuggerRunnerSettings();
    }

    @Nullable
    @Override
    public ConfigurationPerRunnerSettings getConfigurationSettings() {
      return null;
    }
  }
}
