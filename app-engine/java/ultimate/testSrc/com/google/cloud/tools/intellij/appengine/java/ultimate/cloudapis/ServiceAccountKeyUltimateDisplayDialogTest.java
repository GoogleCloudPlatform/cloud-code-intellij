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

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.project.CloudProject;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.JavaPatchableProgramRunner;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.javaee.run.configuration.CommonStrategy;
import com.intellij.javaee.run.configuration.JavaeeRunConfigurationCommonSettingsBean;
import com.intellij.javaee.run.configuration.RunnerSpecificLocalConfigurationBit;
import com.intellij.javaee.run.localRun.ScriptHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.swing.JTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link AppEngineLocalRunServiceAccountKeyRuntimeConfigurationProvider}. */
public class ServiceAccountKeyUltimateDisplayDialogTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;
  @Mock @TestService private RunnerRegistry mockRunnerRegistry;
  @Mock private CloudProject mockCloudProject;
  @Mock private RunnerAndConfigurationSettings mockRunnerAndConfigurationSettings;
  @Mock private RunConfiguration mockRunConfiguration;
  @Mock private JavaPatchableProgramRunner mockProgramRunner;
  @Mock private CommonStrategy mockCommonStrategy;
  @Mock private ScriptHelper mockScriptHelper;
  private RunnerSpecificLocalConfigurationBit runnerSpecificLocalConfigurationBit;

  @Before
  public void setUp() {
    runnerSpecificLocalConfigurationBit =
        new RunnerSpecificLocalConfigurationBit(new TestConfigurationInfoProvider());
    runnerSpecificLocalConfigurationBit.setEnvironmentVariables(new ArrayList<>());

    when(mockRunnerAndConfigurationSettings.getName()).thenReturn("name");
    when(mockRunnerAndConfigurationSettings.getConfiguration()).thenReturn(mockRunConfiguration);

    when(mockRunnerAndConfigurationSettings.getConfigurationSettings(mockProgramRunner))
        .thenReturn(runnerSpecificLocalConfigurationBit);
    when(mockRunnerRegistry.getRunner(any(String.class), any(RunProfile.class)))
        .thenReturn(mockProgramRunner);

    TestConfigurationInfoProvider configurationInfoProvider = new TestConfigurationInfoProvider();
    when(mockCommonStrategy.createStartupHelper(configurationInfoProvider))
        .thenReturn(mockScriptHelper);
    when(mockCommonStrategy.createShutdownHelper(configurationInfoProvider))
        .thenReturn(mockScriptHelper);
    when(mockCommonStrategy.getSettingsBean())
        .thenReturn(new JavaeeRunConfigurationCommonSettingsBean());
  }

  @Test
  public void runConfigurationTable_whenConfigurationsDoNotExist_Hidden() {
    launchDialog(new ArrayList<>());
    assertFalse(dialog.getRunConfigurationTable().isVisible());
  }

  @Test
  public void runConfigurationTable_whenConfigurationsExist_Visible() {
    launchDialog(Arrays.asList(mockRunnerAndConfigurationSettings));
    assertTrue(dialog.getRunConfigurationTable().isVisible());
  }

  @Test
  public void runConfigurationTable_verifyValues() {
    launchDialog(Arrays.asList(mockRunnerAndConfigurationSettings));

    JTable runConfigurationTable = dialog.getRunConfigurationTable();
    assertEquals(1, runConfigurationTable.getRowCount());
    assertEquals(2, runConfigurationTable.getColumnCount());
    assertEquals(
        mockRunnerAndConfigurationSettings, runConfigurationTable.getModel().getValueAt(0, 0));
    assertEquals(true, runConfigurationTable.getModel().getValueAt(0, 1));
  }

  @Test
  public void addEnvironmentVariablesToConfiguration_whenEnvVarsDoNotExistInConfig_add() {
    launchDialog(Arrays.asList(mockRunnerAndConfigurationSettings));
    dialog.addEnvironmentVariablesToConfiguration(
        new HashSet<>(Arrays.asList(mockRunnerAndConfigurationSettings)));

    List<EnvironmentVariable> actualEnvVars = runnerSpecificLocalConfigurationBit.getEnvVariables();
    assertNotNull(actualEnvVars);
    assertEquals(2, actualEnvVars.size());
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars, new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", "gcpProjectId", false)));
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars,
            new EnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "downloadPath", false)));
  }

  @Test
  public void addEnvironmentVariablesToConfiguration_whenEnvVarsExistInConfig_update() {
    List<EnvironmentVariable> oldEnvVariables = new ArrayList<>();
    oldEnvVariables.add(new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", "oldCloudProject", false));
    oldEnvVariables.add(
        new EnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "oldCredentialPath", false));
    oldEnvVariables.add(new EnvironmentVariable("fakeName", "fakeValue", false));
    runnerSpecificLocalConfigurationBit.setEnvironmentVariables(oldEnvVariables);

    launchDialog(Arrays.asList(mockRunnerAndConfigurationSettings));
    dialog.addEnvironmentVariablesToConfiguration(
        new HashSet<>(Arrays.asList(mockRunnerAndConfigurationSettings)));

    List<EnvironmentVariable> actualEnvVars = runnerSpecificLocalConfigurationBit.getEnvVariables();
    assertNotNull(actualEnvVars);
    assertEquals(3, actualEnvVars.size());
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars, new EnvironmentVariable("GOOGLE_CLOUD_PROJECT", "gcpProjectId", false)));
    assertTrue(
        containsEnvironmentVariable(
            actualEnvVars,
            new EnvironmentVariable("GOOGLE_APPLICATION_CREDENTIALS", "downloadPath", false)));
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

  private void launchDialog(List<RunnerAndConfigurationSettings> configurationSettingsList) {
    dialog.configurationSettingsList = configurationSettingsList;
    when(mockCloudProject.projectId()).thenReturn("gcpProjectId");
    ApplicationManager.getApplication()
        .invokeAndWait(
            () ->
                dialog =
                    new ServiceAccountKeyUltimateDisplayDialog(
                        testFixture.getProject(), mockCloudProject, "downloadPath"));
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
