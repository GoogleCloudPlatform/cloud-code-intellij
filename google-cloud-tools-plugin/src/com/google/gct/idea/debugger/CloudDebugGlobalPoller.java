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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger.Debuggees;
import com.google.api.services.clouddebugger.Clouddebugger.Debugger.Debuggees.Breakpoints;
import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.ListBreakpointsResponse;
import com.google.gct.idea.util.GctBundle;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;

/**
 * Queries multiple states on a fixed interval for updates. It notifies listeners
 * when updates occur.
 */
public class CloudDebugGlobalPoller {

  private static final int DELAY_MS = 5000;
  private static final Logger LOG = Logger.getInstance(CloudDebugGlobalPoller.class);

  /**
   * Display group used to display notifications in the IDE
   */
  public static final String CLOUD_DEBUGGER_ERROR_NOTIFICATIONS_DISPLAY_GROUP =
      "Cloud Debugger Error Notifications";

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
      myWatchTimer = new Timer("cloud debug watcher", true /* isDaemon */);
      myWatchTimer.schedule(new CloudDebugGlobalPollerTimerTask(this), DELAY_MS, DELAY_MS);

      ApplicationManager.getApplication().addApplicationListener(new ApplicationAdapter() {
        @Override
        public void applicationExiting() {
          if (myWatchTimer != null) {
            myWatchTimer.cancel();
          }
        }
      });
    }
  }

  public synchronized void stopBackgroundListening() {
    if (myWatchTimer != null) {
      myWatchTimer.cancel();
      myWatchTimer = null;
    }
  }


  List<CloudDebugProcessState> getBackgroundListeningStates() {
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
          CloudDebugProcessState state = cloudConfig.getProcessState();
          if (state != null && state.isListenInBackground()) {
            states.add(state);
          }
        }
      }
    }
    return states;
  }

  private void queryServerForBreakpoints(CloudDebugProcessState state, Debugger client)
      throws IOException {
    if (state.getDebuggeeId() == null) {
      throw new IllegalStateException("CloudDebugProcessState.getDebuggeeId() was null");
    }
    Debuggees debuggees = client.debuggees();
    Breakpoints breakpoints = debuggees.breakpoints();
    Breakpoints.List listRequest = breakpoints.list(state.getDebuggeeId())
        .setIncludeInactive(Boolean.TRUE)
        .setActionValue("CAPTURE")
        .setStripResults(Boolean.TRUE)
        .setWaitToken(state.getWaitToken());

    ListBreakpointsResponse response = listRequest.execute();
    List<Breakpoint> currentList = response.getBreakpoints();
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
   * pollForChanges sends a synchronous, non-hanging query to the server and compares the result to
   * see if there are changes from the current state.
   *
   * @param state represents the target debuggee to query
   */
  void pollForChanges(@NotNull final CloudDebugProcessState state) {
    final Debugger client = CloudDebuggerClient.getShortTimeoutClient(state);
    if (client == null) {
      if (state.isListenInBackground()) {
        // state is supposed to listen, but does not have access to the backend
        LOG.warn("CloudDebugProcessState is listening in the background but no debugger client "
            + "could be retrieved => stop listening");
        handleBreakpointQueryError(state,
                                   GctBundle.message("clouddebug.background.listener.access.error.message",
                                                     state.getProject().getName()));
        return;
      } else {
        // We should poll only states that listen in the background, reaching this branch is
        // unexpected
        LOG.error("Polling changes for a debug state that is not set to listen in the background");
        return;
      }
    }

    boolean changed = false;
    try {
      String oldToken = state.getWaitToken();

      queryServerForBreakpoints(state, client);

      String responseWaitToken = state.getWaitToken();
      if (!Strings.isNullOrEmpty(responseWaitToken)) {
        changed = oldToken == null || !responseWaitToken.equals(oldToken);
      }
      else {
        changed = !Strings.isNullOrEmpty(oldToken);
      }
    }
    catch(SocketTimeoutException sto) {
      //noop, this is expected behavior.
    }
    catch (IOException ex) {
      LOG.warn("exception listing breakpoints", ex);
      handleBreakpointQueryError(state, ex);
      return;
    } catch (Exception e) {
      LOG.error("exception listing breakpoints", e);
      handleBreakpointQueryError(state, e);
      return;
    }

    if (changed) {
      fireBreakpointsChanged(state);
    }
  }

  private void handleBreakpointQueryError(@NotNull CloudDebugProcessState state, @NotNull Exception e) {
    String message;
    String projectName = state.getProject().getName();
    if (e instanceof GoogleJsonResponseException) {
      GoogleJsonResponseException jsonResponseException = (GoogleJsonResponseException) e;
      if (jsonResponseException.getStatusCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN
          || jsonResponseException.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNAUTHORIZED) {
        message = GctBundle.message("clouddebug.background.listener.access.error.message",
                                    projectName);
      } else {
        message = GctBundle.message("clouddebug.background.listener.general.error.message",
                                    projectName,
                                    jsonResponseException.getDetails().getMessage());
      }
    } else {
      message = GctBundle.message("clouddebug.background.listener.general.error.message",
                                  projectName,
                                  e.getLocalizedMessage());
    }
    handleBreakpointQueryError(state, message);
  }

  private void handleBreakpointQueryError(@NotNull CloudDebugProcessState state, String message) {
    state.setListenInBackground(false);
    String title = GctBundle.message("clouddebug.background.listener.error.title");
    Notification notification =
        new Notification(CLOUD_DEBUGGER_ERROR_NOTIFICATIONS_DISPLAY_GROUP,
                         title,
                         message,
                         NotificationType.ERROR);
    Notifications.Bus.notify(notification, state.getProject());
  }
}
