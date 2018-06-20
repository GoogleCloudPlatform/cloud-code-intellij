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

/** Tests for {@link AppEngineLocalRunServiceAccountKeyRuntimeConfigurationProvider}. */
public class ServiceAccountKeyUltimateDisplayDialogTest {

  /*
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
  }*/
}
