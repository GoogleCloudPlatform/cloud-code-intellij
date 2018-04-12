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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger.Debuggees;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger.Debuggees.Breakpoints;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.ListBreakpointsResponse;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationAdapter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtil;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import org.jetbrains.annotations.NotNull;

/**
 * Queries multiple states on a fixed interval for updates. It notifies listeners when updates
 * occur.
 */
public class CloudDebugGlobalPoller {

  private static final int DELAY_MS = 5000;
  private static final Logger LOG = Logger.getInstance(CloudDebugGlobalPoller.class);

  /** Display group used to display notifications in the IDE. */
  public static final String CLOUD_DEBUGGER_ERROR_NOTIFICATIONS_DISPLAY_GROUP =
      "Cloud Debugger Error Notifications";

  private final List<CloudBreakpointListener> breakpointListChangedListeners =
      new ArrayList<CloudBreakpointListener>();
  private Timer watchTimer = null;

  public void addListener(@NotNull CloudBreakpointListener listener) {
    breakpointListChangedListeners.add(listener);
  }

  public void removeListener(@NotNull CloudBreakpointListener listener) {
    breakpointListChangedListeners.remove(listener);
  }

  /** Begins listening on changes in the background. */
  public synchronized void startBackgroundListening() {
    if (watchTimer == null) {
      watchTimer = new Timer("cloud debug watcher", true /* isDaemon */);
      watchTimer.schedule(new CloudDebugGlobalPollerTimerTask(this), DELAY_MS, DELAY_MS);

      ApplicationManager.getApplication()
          .addApplicationListener(
              new ApplicationAdapter() {
                @Override
                public void applicationExiting() {
                  if (watchTimer != null) {
                    watchTimer.cancel();
                  }
                }
              });
    }
  }

  /** Stops listening on changes in the background. */
  public synchronized void stopBackgroundListening() {
    if (watchTimer != null) {
      watchTimer.cancel();
      watchTimer = null;
    }
  }

  private void queryServerForBreakpoints(CloudDebugProcessState state, Debugger client)
      throws IOException {
    if (state.getDebuggeeId() == null) {
      throw new IllegalStateException("CloudDebugProcessState.getDebuggeeId() was null");
    }
    Debuggees debuggees = client.debuggees();
    Breakpoints breakpoints = debuggees.breakpoints();
    Breakpoints.List listRequest =
        breakpoints
            .list(state.getDebuggeeId())
            .setIncludeInactive(Boolean.TRUE)
            .setActionValue("CAPTURE")
            .setStripResults(Boolean.TRUE)
            .setWaitToken(state.getWaitToken());

    ListBreakpointsResponse response =
        listRequest
            .setClientVersion(
                ServiceManager.getService(PluginInfoService.class)
                    .getClientVersionForCloudDebugger())
            .execute();
    List<Breakpoint> currentList = response.getBreakpoints();
    String responseWaitToken = response.getNextWaitToken();
    state.setWaitToken(responseWaitToken);

    if (currentList != null) {
      Collections.sort(currentList, BreakpointComparer.getDefaultInstance());
    }

    state.setCurrentServerBreakpointList(
        currentList != null
            ? ContainerUtil.immutableList(currentList)
            : ContainerUtil.immutableList(new ArrayList<Breakpoint>()));
  }

  private void fireBreakpointsChanged(@NotNull CloudDebugProcessState state) {
    for (CloudBreakpointListener listener : breakpointListChangedListeners) {
      listener.onBreakpointListChanged(state);
    }
  }

  /**
   * pollForChanges sends a synchronous, hanging query to the server and compares the result to see
   * if there are changes from the current state. Explanation of <a
   * href="https://en.wikipedia.org/wiki/Push_technology#Long_polling">hanging query (a.k.a. long
   * poll)</a>
   *
   * @param state represents the target debuggee to query
   */
  void pollForChanges(@NotNull final CloudDebugProcessState state) {
    final Debugger client = CloudDebuggerClient.getShortTimeoutClient(state);
    if (client == null) {
      if (state.isListenInBackground()) {
        // state is supposed to listen, but does not have access to the backend
        LOG.warn(
            "CloudDebugProcessState is listening in the background but no debugger client "
                + "could be retrieved => stop listening");
        handleBreakpointQueryError(
            state,
            StackdriverDebuggerBundle.message(
                "clouddebug.background.listener.access.error.message",
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
      } else {
        changed = !Strings.isNullOrEmpty(oldToken);
      }
    } catch (SocketTimeoutException ex) {
      // noop, this is expected behavior.
    } catch (GoogleJsonResponseException ex) {
      // HTTP 409 is expected when backend responds to "hanging query" either for timeout or because
      // a result is available (which will be retrieved via the subsequent query)
      if (ex.getStatusCode() != HttpURLConnection.HTTP_CONFLICT) {
        handleBreakpointQueryError(state, ex);
      }
    } catch (IOException ex) {
      LOG.warn("exception listing breakpoints", ex);
      handleBreakpointQueryError(state, ex);
      return;
    } catch (Exception ex) {
      LOG.error("exception listing breakpoints", ex);
      handleBreakpointQueryError(state, ex);
      return;
    }

    if (changed) {
      fireBreakpointsChanged(state);
    }
  }

  private void handleBreakpointQueryError(
      @NotNull CloudDebugProcessState state, @NotNull Exception ex) {
    String message;
    String projectName = state.getProject().getName();
    if (ex instanceof GoogleJsonResponseException) {
      GoogleJsonResponseException jsonResponseException = (GoogleJsonResponseException) ex;
      if (jsonResponseException.getStatusCode() == HttpURLConnection.HTTP_FORBIDDEN
          || jsonResponseException.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        message =
            StackdriverDebuggerBundle.message(
                "clouddebug.background.listener.access.error.message", projectName);
      } else {
        message =
            StackdriverDebuggerBundle.message(
                "clouddebug.background.listener.general.error.message",
                projectName,
                jsonResponseException.getDetails().getMessage());
      }
    } else {
      message =
          StackdriverDebuggerBundle.message(
              "clouddebug.background.listener.general.error.message",
              projectName,
              ex.getLocalizedMessage());
    }
    handleBreakpointQueryError(state, message);
  }

  private void handleBreakpointQueryError(@NotNull CloudDebugProcessState state, String message) {
    state.setListenInBackground(false);
    String title = StackdriverDebuggerBundle.message("clouddebug.background.listener.error.title");
    Notification notification =
        new Notification(
            CLOUD_DEBUGGER_ERROR_NOTIFICATIONS_DISPLAY_GROUP,
            title,
            message,
            NotificationType.ERROR);
    Notifications.Bus.notify(notification, state.getProject());
  }
}
