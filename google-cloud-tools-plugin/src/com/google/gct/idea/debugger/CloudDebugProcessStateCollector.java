/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.debugger;

import com.google.common.annotations.VisibleForTesting;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Retrieves {@link CloudDebugProcessState} objects from application run configurations
 */
public class CloudDebugProcessStateCollector {

  public static CloudDebugProcessStateCollector getInstance() {
    return ServiceManager.getService(CloudDebugProcessStateCollector.class);
  }

  public List<CloudDebugProcessState> getBackgroundListeningStates() {
    List<CloudDebugProcessState> states = new ArrayList<CloudDebugProcessState>();

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      Set<RunProfile> runningProfiles = getProfilesWithActiveDebugSession(project);

      RunManager manager = RunManager.getInstance(project);

      // find all CloudDebugRunConfiguration that do not have active debug sessions but are
      // listening in the background
      for (final RunnerAndConfigurationSettings config : manager.getAllSettings()) {
        if (notRunningConfiguration(runningProfiles, config.getConfiguration())) {
          if (config.getConfiguration() instanceof CloudDebugRunConfiguration) { // NOPMD
            final CloudDebugRunConfiguration cloudConfig =
                (CloudDebugRunConfiguration) config.getConfiguration();
            CloudDebugProcessState state = cloudConfig.getProcessState();
            if (listensInBackground(state)) {
              states.add(state);
            }
          }
        }
      }
    }
    return states;
  }

  @NotNull
  @VisibleForTesting
  Set<RunProfile> getProfilesWithActiveDebugSession(Project project) {
    Set<RunProfile> debuggingProfiles = new HashSet<RunProfile>();
    XDebuggerManager debugManager = XDebuggerManager.getInstance(project);
    for (XDebugSession session : debugManager.getDebugSessions()) {
      if (notStoppedAndHasRunProfile(session)) {
        debuggingProfiles.add(session.getRunProfile());
      }
    }
    return debuggingProfiles;
  }

  @VisibleForTesting
  boolean notStoppedAndHasRunProfile(XDebugSession session) {
    return !session.isStopped() && session.getRunProfile() != null;
  }

  @VisibleForTesting
  boolean notRunningConfiguration(Set<RunProfile> runningConfigurations, RunConfiguration config) {
    return config != null && !runningConfigurations.contains(config);
  }

  @VisibleForTesting
  boolean listensInBackground(CloudDebugProcessState state) {
    return state != null && state.isListenInBackground();
  }

}
