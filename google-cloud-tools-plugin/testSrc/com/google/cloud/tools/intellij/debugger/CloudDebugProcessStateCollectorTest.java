/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

public class CloudDebugProcessStateCollectorTest extends BasePluginTestCase {

  @Test
  public void testGetBackgroundListeningStates_withMultipleProjects() {
    int runningSessions = 1;
    int listeningSessions = 2;
    int notListeningSessions = 3;

    Project project1 = createProject(runningSessions, listeningSessions, notListeningSessions);
    Project project2 = createProject(runningSessions, listeningSessions, notListeningSessions);

    createMockProjectManagerWithProjects(new Project[]{ project1, project2 });

    List<CloudDebugProcessState> backgroundListeningStates =
        new CloudDebugProcessStateCollector().getBackgroundListeningStates();

    assertNotNull(backgroundListeningStates);
    assertThat(backgroundListeningStates).hasSize(2 * listeningSessions);
    for (CloudDebugProcessState state : backgroundListeningStates) {
      assertTrue(state.isListenInBackground());
    }
  }

  @Test
  public void testGetBackgroundListeningStates_returnsEmptyListWhenNoProjectIsOpen() {
    createMockProjectManagerWithProjects(new Project[0]);

    List<CloudDebugProcessState> backgroundListeningStates =
        new CloudDebugProcessStateCollector().getBackgroundListeningStates();
    assertNotNull(backgroundListeningStates);
    assertThat(backgroundListeningStates).hasSize(0);
  }

  @Test
  public void testGetProfilesWithActiveDebugSession_returnsEmptySetIfNoDebugSessions() {
    Project project = mock(Project.class);
    createMockXDebuggerManager(project, new XDebugSession[0]);

    Set<RunProfile> profiles =
        new CloudDebugProcessStateCollector().getProfilesWithActiveDebugSession(project);

    assertNotNull(profiles);
    assertThat(profiles).hasSize(0);
  }

  @Test
  public void testGetProfilesWithActiveDebugSession_returnsNotStoppedSessionsWithRunProfile() {
    Project project = mock(Project.class);

    XDebugSession notStoppedSession =
        createMockSession(false, mock(CloudDebugRunConfiguration.class));
    XDebugSession stoppedSession = createMockSession(true, mock(CloudDebugRunConfiguration.class));
    XDebugSession stoppedSessionWithoutRunProfile = createMockSession(true, null);

    createMockXDebuggerManager(project, new XDebugSession[]{ notStoppedSession,
                                                             stoppedSession,
                                                             stoppedSessionWithoutRunProfile});

    Set<RunProfile> profiles =
        new CloudDebugProcessStateCollector().getProfilesWithActiveDebugSession(project);

    assertNotNull(profiles);
    assertThat(profiles).hasSize(1);
  }

  @Test
  public void testNotStoppedAndHasRunProfile_returnsTrueIfNotStoppedAndHasProfile() {
    XDebugSession session = createMockSession(false, mock(RunProfile.class));

    assertTrue(new CloudDebugProcessStateCollector().notStoppedAndHasRunProfile(session));
  }

  @Test
  public void testNotStoppedAndHasRunProfile_returnsFalseIfStoppedAndHasProfile() {
    XDebugSession session = createMockSession(true, mock(RunProfile.class));

    assertFalse(new CloudDebugProcessStateCollector().notStoppedAndHasRunProfile(session));
  }

  @Test
  public void testNotStoppedAndHasRunProfile_returnsFalseIfNotStoppedAndHasNoProfile() {
    XDebugSession session = createMockSession(false, null);

    assertFalse(new CloudDebugProcessStateCollector().notStoppedAndHasRunProfile(session));
  }

  @Test
  public void testNotStoppedAndHasRunProfile_returnsFalseIfStoppedAndHasNoProfile() {
    XDebugSession session = createMockSession(true, null);

    assertFalse(new CloudDebugProcessStateCollector().notStoppedAndHasRunProfile(session));
  }

  @Test
  public void testNotRunningConfiguration_returnsTrueIfThereAreNoRunningConfigurations() {
    RunConfiguration config = mock(RunConfiguration.class);

    assertTrue(new CloudDebugProcessStateCollector()
        .notRunningConfiguration(new HashSet<RunProfile>(), config));
  }

  @Test
  public void testNotRunningConfiguration_returnsFalseIfNullConfigIsPassedIn() {
    assertFalse(new CloudDebugProcessStateCollector()
        .notRunningConfiguration(new HashSet<RunProfile>(), null));
  }

  @Test
  public void testNotRunningConfiguration_returnsFalseIfConfigIsInRunningConfigurations() {
    RunConfiguration config = mock(RunConfiguration.class);
    HashSet<RunProfile> runningConfigurations = new HashSet<RunProfile>();
    runningConfigurations.add(config);

    assertFalse(new CloudDebugProcessStateCollector().notRunningConfiguration(
        runningConfigurations, config));
  }

  @Test
  public void testListensInBackground_returnsFalseIfNullIsPassedIn() throws Exception {
    assertFalse(new CloudDebugProcessStateCollector().listensInBackground(null));
  }

  @Test
  public void testListensInBackground_returnsFalseIfStateDoesNotListenInBackground() throws Exception {
    CloudDebugProcessState state = new CloudDebugProcessState();
    state.setListenInBackground(false);

    assertFalse(new CloudDebugProcessStateCollector().listensInBackground(state));
  }

  @Test
  public void testListensInBackground_returnsTrueIfStateListensInBackground() throws Exception {
    CloudDebugProcessState state = new CloudDebugProcessState();
    state.setListenInBackground(true);

    assertTrue(new CloudDebugProcessStateCollector().listensInBackground(state));
  }

  private Project createProject(int inProgressDebugSessions,
                                int backgroundListeningDebugsSessions,
                                int notListeningDebugSessions) {
    XDebuggerManager debuggerManager = mock(XDebuggerManager.class);
    XDebugSession[] debugSessions = new XDebugSession[inProgressDebugSessions];
    List<RunnerAndConfigurationSettings> allRunnerSettings =
        new ArrayList<RunnerAndConfigurationSettings>();
    for (int i = 0; i < inProgressDebugSessions; i++) {
      XDebugSession debugSession = createInProgressDebugSettings(allRunnerSettings);

      debugSessions[i] = debugSession;
    }

    when(debuggerManager.getDebugSessions()).thenReturn(debugSessions);
    applicationContainer.unregisterComponent(XDebuggerManager.class.getName());
    registerService(XDebuggerManager.class, debuggerManager);

    for (int i = 0; i < backgroundListeningDebugsSessions; i++) {
      createBackgroundListeningDebugSettings(allRunnerSettings);
    }

    for (int i = 0; i < notListeningDebugSessions; i++) {
      createNotListeningNotActiveSettings(allRunnerSettings);
    }

    RunManager runManager = mock(RunManager.class);
    when(runManager.getAllSettings()).thenReturn(allRunnerSettings);
    applicationContainer.unregisterComponent(RunManager.class.getName());
    registerService(RunManager.class, runManager);

    return project;
  }

  void createMockProjectManagerWithProjects(Project[] value) {
    ProjectManager projectManager = mock(ProjectManager.class);
    when(projectManager.getOpenProjects()).thenReturn(value);
    registerService(ProjectManager.class, projectManager);
  }

  void createMockXDebuggerManager(Project project, XDebugSession[] value) {
    XDebuggerManager debuggerManager = mock(XDebuggerManager.class);
    when(debuggerManager.getDebugSessions()).thenReturn(value);
    when(project.getComponent(XDebuggerManager.class)).thenReturn(debuggerManager);
  }

  private void createBackgroundListeningDebugSettings(
      List<RunnerAndConfigurationSettings> allRunnerSettings) {
    createDebugSettingsAndAddToRunnerSettings(allRunnerSettings,
                       /* isStopped */ true,
                       /* hasDebugSession */ false,
                       /* hasProcessState */ Boolean.TRUE);
  }

  private void createNotListeningNotActiveSettings(
      List<RunnerAndConfigurationSettings> allRunnerSettings) {
    createDebugSettingsAndAddToRunnerSettings(allRunnerSettings,
                       /* isStopped */ true,
                       /* hasDebugSession */ false,
                       /* hasProcessState */ null);
  }

  @Nullable
  private XDebugSession createInProgressDebugSettings(
      List<RunnerAndConfigurationSettings> allRunnerSettings) {
    return createDebugSettingsAndAddToRunnerSettings(allRunnerSettings,
                              /* isStopped */ false,
                              /* hasDebugSession */ true,
                              /* hasProcessState */ Boolean.FALSE);
  }

  private XDebugSession createDebugSettingsAndAddToRunnerSettings(
      List<RunnerAndConfigurationSettings> runnerSettings,
      boolean isStopped,
      boolean hasDebugSession,
      Boolean listenInBackground) {

    CloudDebugRunConfiguration runConfiguration = mock(CloudDebugRunConfiguration.class);

    if (listenInBackground != null) {
      CloudDebugProcessState processState = new CloudDebugProcessState();
      processState.setListenInBackground(listenInBackground);
      when(runConfiguration.getProcessState()).thenReturn(processState);
    }

    RunnerAndConfigurationSettings configurationSettings =
        mock(RunnerAndConfigurationSettings.class);
    when(configurationSettings.getConfiguration()).thenReturn(runConfiguration);

    runnerSettings.add(configurationSettings);

    if (hasDebugSession) {
      return createMockSession(isStopped, runConfiguration);
    } else {
      return null;
    }
  }

  @NotNull
  private XDebugSession createMockSession(boolean isStopped, RunProfile runProfile) {
    XDebugSession debugSession = mock(XDebugSession.class);
    when(debugSession.isStopped()).thenReturn(isStopped);
    if (runProfile != null) {
      when(debugSession.getRunProfile()).thenReturn(runProfile);
    }
    return debugSession;
  }

}
