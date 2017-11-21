/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.GetBreakpointResponse;
import com.google.api.services.clouddebugger.v2.model.ListBreakpointsResponse;
import com.google.api.services.clouddebugger.v2.model.SetBreakpointResponse;
import com.google.api.services.clouddebugger.v2.model.SourceLocation;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * A controller is responsible for keeping one {@link CloudDebugProcessState} object up to date and
 * notifies {@link CloudBreakpointListener} when state changes. It also performs asynchronous
 * operations to retrieve fully hydrated {@link Breakpoint}.
 */
@SuppressWarnings("FutureReturnValueIgnored")
public class CloudDebugProcessStateController {

  private static final int INITIAL_DELAY_MS = 2000;
  private static final Logger LOG = Logger.getInstance(CloudDebugProcessStateController.class);
  private static final int PERIOD_MS = 500;
  private final List<CloudBreakpointListener> breakpointListChangedListeners = new ArrayList<>();
  private final ConcurrentHashMap<String, Breakpoint> fullFinalBreakpoints =
      new ConcurrentHashMap<>();
  private volatile Timer listBreakpointsJob;
  private CloudDebugProcessState state;

  protected CloudDebugProcessStateController() {}

  /**
   * Adds a listener for update events. When the controller detects changes, it will fire an event
   * to all subscribers
   *
   * @param listener the subscriber to receive events
   */
  public void addListener(@NotNull CloudBreakpointListener listener) {
    breakpointListChangedListeners.add(listener);
  }

  void deleteBreakpointAsync(@NotNull final String breakpointId) {
    deleteBreakpoint(breakpointId, true);
  }

  void deleteBreakpoint(@NotNull final String breakpointId) {
    deleteBreakpoint(breakpointId, false);
  }

  /**
   * Called from the {@link CloudBreakpointHandler} to remove breakpoints from the server.
   *
   * @param breakpointId the {@link Breakpoint} Id to delete
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void deleteBreakpoint(@NotNull final String breakpointId, boolean performAsync) {
    if (state == null) {
      throw new IllegalStateException();
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(state);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      Messages.showErrorDialog(
          state.getProject(),
          GctBundle.getString("clouddebug.bad.login.message"),
          GctBundle.getString("clouddebug.message.title"));
      return;
    }
    final String debuggeeId = state.getDebuggeeId();
    assert debuggeeId != null;

    Runnable performDelete =
        () -> {
          try {
            client
                .debuggees()
                .breakpoints()
                .delete(debuggeeId, breakpointId)
                .setClientVersion(
                    ServiceManager.getService(CloudToolsPluginInfoService.class)
                        .getClientVersionForCloudDebugger())
                .execute();
          } catch (IOException ex) {
            LOG.warn("exception deleting breakpoint " + breakpointId, ex);
          }
        };

    if (performAsync) {
      ApplicationManager.getApplication().executeOnPooledThread(performDelete);
    } else {
      performDelete.run();
    }
  }

  /** Fires a change notification to all subscribers. */
  public void fireBreakpointsChanged() {
    for (CloudBreakpointListener listener : breakpointListChangedListeners) {
      listener.onBreakpointListChanged(state);
    }
  }

  /**
   * Binds this controller to a {@link CloudDebugProcessState} and initializes that state from the
   * server.
   *
   * @param state the {@link CloudDebugProcessState} the controller will be bound to
   */
  public void initialize(@NotNull CloudDebugProcessState state) {
    this.state = state;
    state.setWaitToken(null);
    waitForChanges();
  }

  /**
   * Removes the specified listener from the list of subscribers to receive update events.
   *
   * @param listener the subscriber to remove
   */
  public void removeListener(@NotNull CloudBreakpointListener listener) {
    breakpointListChangedListeners.remove(listener);
  }

  /** Returns a fully realized {@link Breakpoint} with all results possibly asynchronously. */
  @SuppressWarnings("FutureReturnValueIgnored")
  public void resolveBreakpointAsync(
      @NotNull final String id, @NotNull final ResolveBreakpointHandler handler) {

    if (fullFinalBreakpoints.containsKey(id)) {
      handler.onSuccess(fullFinalBreakpoints.get(id));
      return;
    }
    if (state == null) {
      handler.onError(GctBundle.getString("clouddebug.invalid.state"));
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(state);
    if (client == null) {
      LOG.warn("no client available attempting to resolveBreakpointAsync");
      handler.onError(GctBundle.getString("clouddebug.bad.login.message"));
      return;
    }
    List<Breakpoint> currentList = state.getCurrentServerBreakpointList();
    for (Breakpoint serverBreakpointCandidate : currentList) {
      if (serverBreakpointCandidate.getId().equals(id)
          && !Boolean.TRUE.equals(serverBreakpointCandidate.getIsFinalState())) {
        handler.onSuccess(serverBreakpointCandidate);
        return;
      }
    }

    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              //At this point, the user has selected a final state breakpoint which is not yet hydrated.
              //So we query the server to get this final on a worker thread and then run the runnable
              // back on ui
              GetBreakpointResponse response;
              try {
                response =
                    client
                        .debuggees()
                        .breakpoints()
                        .get(state.getDebuggeeId(), id)
                        .setClientVersion(
                            ServiceManager.getService(CloudToolsPluginInfoService.class)
                                .getClientVersionForCloudDebugger())
                        .execute();
                Breakpoint result = response.getBreakpoint();
                if (result != null) {
                  fullFinalBreakpoints.put(id, result);
                  handler.onSuccess(result);
                } else {
                  handler.onError(GctBundle.getString("clouddebug.no.response"));
                }
              } catch (IOException ex) {
                LOG.warn(
                    "IOException hydrating a snapshot.  User may have deleted the snapshot", ex);
                handler.onError(ex.toString());
              }
            });
  }

  /** Called from the {@link CloudDebugProcessHandler} to set a breakpoint. */
  @SuppressWarnings("FutureReturnValueIgnored")
  void setBreakpointAsync(
      @NotNull final Breakpoint serverBreakpoint, @NotNull final SetBreakpointHandler handler) {

    if (state == null) {
      handler.onError(GctBundle.getString("clouddebug.invalid.state"));
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(state);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      handler.onError(GctBundle.getString("clouddebug.bad.login.message"));
      return;
    }

    final String debuggeeId = state.getDebuggeeId();
    assert debuggeeId != null;

    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              try {
                // Delete old breakpoints at this location.
                List<Breakpoint> currentList = state.getCurrentServerBreakpointList();
                SourceLocation location = serverBreakpoint.getLocation();
                for (Breakpoint serverBp : currentList) {
                  if (!Boolean.TRUE.equals(serverBp.getIsFinalState())
                      && serverBp.getLocation().getLine() != null
                      && serverBp.getLocation().getLine().equals(location.getLine())
                      && !Strings.isNullOrEmpty(serverBp.getLocation().getPath())
                      && serverBp.getLocation().getPath().equals(location.getPath())) {
                    deleteBreakpoint(serverBp.getId()); //should not be async here.
                  }
                }

                SetBreakpointResponse addResponse =
                    client
                        .debuggees()
                        .breakpoints()
                        .set(debuggeeId, serverBreakpoint)
                        .setClientVersion(
                            ServiceManager.getService(CloudToolsPluginInfoService.class)
                                .getClientVersionForCloudDebugger())
                        .execute();

                if (addResponse != null && addResponse.getBreakpoint() != null) {
                  Breakpoint result = addResponse.getBreakpoint();
                  if (result.getStatus() != null
                      && Boolean.TRUE.equals(result.getStatus().getIsError())
                      && result.getStatus().getDescription() != null) {
                    handler.onError(BreakpointUtil.getUserErrorMessage(result.getStatus()));
                  }
                  handler.onSuccess(addResponse.getBreakpoint().getId());
                } else {
                  handler.onError(GctBundle.getString("clouddebug.no.response"));
                }
              } catch (IOException ex) {
                LOG.error("exception setting a breakpoint", ex);
                handler.onError(ex.toString());
              }
            });
  }

  /** Begins background listening from the server. When changes occur, listeners are notified. */
  public void startBackgroundListening() {
    assert state != null;
    if (listBreakpointsJob == null) {
      listBreakpointsJob = new Timer("list breakpoints");
      final Runnable runnable =
          new Runnable() {
            @Override
            public void run() {
              waitForChanges();
              Timer timer = listBreakpointsJob;
              if (timer != null) {
                try {
                  // We run after a short period to act as a throttle.
                  timer.schedule(new RunnableTimerTask(this), PERIOD_MS);
                } catch (IllegalStateException ex) {
                  //This can happen in rare race conditions and isn't an error.  We just ignore it.
                }
              }
            }
          };
      listBreakpointsJob.schedule(new RunnableTimerTask(runnable), INITIAL_DELAY_MS);
    }
  }

  /** Stops background listening. */
  public void stopBackgroundListening() {
    if (listBreakpointsJob != null) {
      listBreakpointsJob.cancel();
    }
    listBreakpointsJob = null;
  }

  boolean isBackgroundListening() {
    return listBreakpointsJob != null;
  }

  /** Package protected for test purposes only. */
  void waitForChanges() {
    if (state == null) {
      LOG.error("no state available attempting to checkForChanges");
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(state);
    if (client == null) {
      LOG.info("no client available attempting to checkForChanges");
      return;
    }

    String tokenToSend = state.getWaitToken();
    List<Breakpoint> currentList;
    try {
      currentList = queryServerForBreakpoints(state, client, tokenToSend);
    } catch (SocketTimeoutException ex) {
      // Timeout is expected on a hanging get.
      return;
    } catch (GoogleJsonResponseException ex) {
      // A 409 JsonResponseException is used by the server to indicate to us a change happened and
      // we need to requery.
      if (ex.getDetails().getCode() == 409) {
        try {
          currentList = queryServerForBreakpoints(state, client, tokenToSend);
        } catch (IOException ioException) {
          LOG.warn("exception listing breakpoints", ioException);
          return;
        }
      } else {
        LOG.warn("exception listing breakpoints", ex);
        return;
      }
    } catch (IOException ex) {
      LOG.warn("exception listing breakpoints", ex);
      return;
    }

    if (!isBackgroundListening()) {
      return;
    }

    //tokenToSend can be null on first initialization -- where we shouldn't fire events or need
    // to do pruning.
    if (!Strings.isNullOrEmpty(tokenToSend)) {
      pruneBreakpointCache(currentList);
      fireBreakpointsChanged();
    }
  }

  private List<Breakpoint> queryServerForBreakpoints(
      CloudDebugProcessState state, Debugger client, String tokenToSend) throws IOException {
    List<Breakpoint> currentList = null;

    String responseWaitToken = tokenToSend;

    while (tokenToSend == null || tokenToSend.equals(responseWaitToken)) {
      if (tokenToSend != null && !isBackgroundListening()) {
        return null;
      }

      ListBreakpointsResponse response =
          client
              .debuggees()
              .breakpoints()
              .list(state.getDebuggeeId())
              .setIncludeInactive(Boolean.TRUE)
              .setActionValue("CAPTURE")
              .setStripResults(Boolean.TRUE)
              .setWaitToken(CloudDebugConfigType.useWaitToken() ? tokenToSend : null)
              .setClientVersion(
                  ServiceManager.getService(CloudToolsPluginInfoService.class)
                      .getClientVersionForCloudDebugger())
              .execute();

      //We are running on a background thread and the cancel can happen any time triggered
      //on the ui thread from the user.  We want to short circuit immediately and not change
      //any state.  If we processed this result, it could incorrectly update the state and mess
      //up the background watcher.
      if (tokenToSend != null && !isBackgroundListening()) {
        return null;
      }

      currentList = response.getBreakpoints();
      responseWaitToken = response.getNextWaitToken();
      if (tokenToSend == null) {
        break;
      }

      if (!CloudDebugConfigType.useWaitToken() && tokenToSend.equals(responseWaitToken)) {
        try {
          //our fallback polling mode has a 1 second loop.
          Thread.currentThread().sleep(1000);
        } catch (InterruptedException ex) {
          return null;
        }
      }
    }
    state.setWaitToken(responseWaitToken);

    if (currentList != null) {
      Collections.sort(currentList, BreakpointComparer.getDefaultInstance());
    }

    state.setCurrentServerBreakpointList(
        currentList != null
            ? ContainerUtil.immutableList(currentList)
            : ContainerUtil.immutableList(new ArrayList<>()));

    return currentList;
  }

  private void pruneBreakpointCache(List<Breakpoint> currentList) {
    //Clear out the obsolete breakpoint cache for old items.
    HashSet<String> toRemoveSet = new HashSet<>();
    toRemoveSet.addAll(fullFinalBreakpoints.keySet());
    if (!toRemoveSet.isEmpty() && currentList != null) {
      for (Breakpoint bp : currentList) {
        toRemoveSet.remove(bp.getId());
      }
    }

    for (String idToRemove : toRemoveSet) {
      fullFinalBreakpoints.remove(idToRemove);
    }
  }

  interface SetBreakpointHandler {

    void onSuccess(@NotNull String newBreakpointId);

    void onError(String errorMessage);
  }

  interface ResolveBreakpointHandler {

    void onSuccess(@NotNull Breakpoint newBreakpoint);

    void onError(String errorMessage);
  }

  static class RunnableTimerTask extends TimerTask {

    private final Runnable runnable;

    public RunnableTimerTask(@NotNull Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void run() {
      runnable.run();
    }
  }
}
