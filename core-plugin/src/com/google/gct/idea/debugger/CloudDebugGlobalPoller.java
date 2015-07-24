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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.ListBreakpointsResponse;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * The {@link CloudDebugGlobalPoller} queries multiple states on a fixed interval for updates. It notifies listeners
 * when updates occur.
 */
public class CloudDebugGlobalPoller {
  private static final int DELAY_MS = 10000;
  private static final Logger LOG = Logger.getInstance(CloudDebugGlobalPoller.class);
  private final List<CloudBreakpointListener> myBreakpointListChangedListeners =
    new ArrayList<CloudBreakpointListener>();
  private Timer myWatchTimer = null;

  public void addListener(@NotNull CloudBreakpointListener listener) {
    myBreakpointListChangedListeners.add(listener);
  }

  public void removeListener(@NotNull CloudBreakpointListener listener) {
    myBreakpointListChangedListeners.remove(listener);
  }

  /**
   * Begins listening on changes in the background.
   */
  public synchronized void startBackgroundListening() {
    if (myWatchTimer == null) {
      myWatchTimer = new Timer("cloud debug watcher");
      myWatchTimer.schedule(new TimerTask() {
        @Override
        public void run() {
          for (CloudDebugProcessState state : getStates()) {
            pollForChanges(state);
          }
        }
      }, DELAY_MS, DELAY_MS);

      ApplicationManager.getApplication().addApplicationListener(new ApplicationAdapter() {
        @Override
        public void applicationExiting() {
          myWatchTimer.cancel();
        }
      });
    }
  }

  private static List<CloudDebugProcessState> getStates() {
    List<CloudDebugProcessState> states = new ArrayList<CloudDebugProcessState>();

    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      Set<RunProfile> debuggingProfiles = new HashSet<RunProfile>();
      XDebuggerManager debugManager = XDebuggerManager.getInstance(project);
      for (XDebugSession session : debugManager.getDebugSessions()) {
        if (!session.isStopped() && session.getRunProfile() != null) {
          debuggingProfiles.add(session.getRunProfile());
        }
      }

      RunManager manager = RunManager.getInstance(project);
      for (final RunnerAndConfigurationSettings config : manager.getAllSettings()) {
        if (config.getConfiguration() == null || debuggingProfiles.contains(config.getConfiguration())) {
          continue;
        }

        if (config.getConfiguration() instanceof CloudDebugRunConfiguration) {
          final CloudDebugRunConfiguration cloudConfig = (CloudDebugRunConfiguration)config.getConfiguration();
          if (cloudConfig.isShowNotifications()) {
            CloudDebugProcessState state = cloudConfig.getProcessState();
            if (state != null) {
              states.add(state);
            }
          }
        }
      }
    }
    return states;
  }

  private static void queryServerForBreakpoints(CloudDebugProcessState state, Debugger client) throws IOException {
    List<Breakpoint> currentList;
    ListBreakpointsResponse response =
      client.debuggees().breakpoints().list(state.getDebuggeeId()).setIncludeInactive(Boolean.TRUE)
          .setActionValue("CAPTURE").setStripResults(Boolean.TRUE).setWaitToken(null).execute();
    currentList = response.getBreakpoints();
    String responseWaitToken = response.getNextWaitToken();
    state.setWaitToken(responseWaitToken);

    if (currentList != null) {
      Collections.sort(currentList, BreakpointComparer.getDefaultInstance());
    }

    state.setCurrentServerBreakpointList(currentList != null
                                         ? ContainerUtil.immutableList(currentList)
                                         : ContainerUtil.immutableList(new ArrayList<Breakpoint>()));
  }

  private void fireBreakpointsChanged(@NotNull CloudDebugProcessState state) {
    for (CloudBreakpointListener listener : myBreakpointListChangedListeners) {
      listener.onBreakpointListChanged(state);
    }
  }

  /**
   * pollForChanges does a synchronous, nonhanging query to the server and compares the result to see if there are
   * changes from the current state.
   *
   * @param state represents the target debuggee to query
   */
  private void pollForChanges(@NotNull final CloudDebugProcessState state) {
    final Debugger client = CloudDebuggerClient.getCloudDebuggerClient(state);
    if (client == null) {
      // It is ok if there is no client.  We may want to consider notifying the user that
      // background polling is disabled until they login, but for now, we can just doc it.
      LOG.info("no client available attempting to checkForChanges");
      return;
    }

    boolean changed;
    try {
      String oldToken = state.getWaitToken();

      queryServerForBreakpoints(state, client);

      String responseWaitToken = state.getWaitToken();
      if (!Strings.isNullOrEmpty(responseWaitToken)) {
        assert responseWaitToken != null;
        changed = oldToken == null || !responseWaitToken.equals(oldToken);
      }
      else {
        changed = !Strings.isNullOrEmpty(oldToken);
      }
    }
    catch (IOException ex) {
      LOG.warn("exception listing breakpoints", ex);
      return;
    }

    if (changed) {
      fireBreakpointsChanged(state);
    }
  }

}
