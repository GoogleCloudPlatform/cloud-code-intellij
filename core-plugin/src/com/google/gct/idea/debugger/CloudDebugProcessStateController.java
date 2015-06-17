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
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.debugger.Debugger;
import com.google.api.services.debugger.model.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A controller is responsible for keeping one {@link CloudDebugProcessState} object up to date and notifies {@link
 * CloudBreakpointListener} when state changes. It also performs asynchronous operations to retrieve fully hydrated
 * {@link Breakpoint}.
 */
public class CloudDebugProcessStateController {
  private static final int INITIAL_DELAY_MS = 2000;
  private static final Logger LOG = Logger.getInstance(CloudDebugProcessStateController.class);
  private static final int PERIOD_MS = 500;
  private final List<CloudBreakpointListener> myBreakpointListChangedListeners =
    new ArrayList<CloudBreakpointListener>();
  private final ConcurrentHashMap<String, Breakpoint> myFullFinalBreakpoints =
    new ConcurrentHashMap<String, Breakpoint>();
  private Timer myListBreakpointsJob;
  private CloudDebugProcessState myState;

  protected CloudDebugProcessStateController() {
  }

  /**
   * Adds a listener for update events.  When the controller detects changes, it will fire an event to all subscribers
   *
   * @param listener the subscriber to receive events
   */
  public void addListener(@NotNull CloudBreakpointListener listener) {
    myBreakpointListChangedListeners.add(listener);
  }

  /**
   * Called from the {@link CloudBreakpointHandler} to remove breakpoints from the server.
   *
   * @param breakpointId the {@link com.google.api.services.debugger.model.Breakpoint} Id to delete
   */
  void deleteBreakpoint(@NotNull String breakpointId) {
    if (myState == null) {
      throw new IllegalStateException();
    }
    final Debugger client = CloudDebuggerClient.getCloudDebuggerClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      Messages.showErrorDialog(myState.getProject(), GctBundle.getString("clouddebug.bad_login_message"),
                               GctBundle.getString("clouddebug.errortitle"));
      return;
    }
    try {
      client.debuggees().breakpoints().delete(myState.getDebuggeeId(), breakpointId).execute();
    }
    catch (IOException ex) {
      LOG.warn("exception deleting breakpoint " + breakpointId, ex);
    }
  }

  /**
   * Fires a change notification to all subscribers.
   */
  public void fireBreakpointsChanged() {
    for (CloudBreakpointListener listener : myBreakpointListChangedListeners) {
      listener.onBreakpointListChanged(myState);
    }
  }

  /**
   * Binds this controller to a {@link CloudDebugProcessState} and initializes that state from the server.
   *
   * @param state the {@link CloudDebugProcessState} the controller will be bound to
   */
  public void initialize(@NotNull CloudDebugProcessState state) {
    myState = state;
    myState.setWaitToken(null);
    waitForChanges();
  }

  /**
   * Removes the specified listener from the list of subscribers to receive update events.
   *
   * @param listener the subscriber to remove
   */
  public void removeListener(@NotNull CloudBreakpointListener listener) {
    myBreakpointListChangedListeners.remove(listener);
  }

  /**
   * Returns a fully realized {@link Breakpoint} with all results possibly asynchronously
   *
   * @param id the breakpoint id to resolve
   * @return a {@link ListenableFuture} that is set once the full breakpoint is loaded
   */
  @Nullable
  public ListenableFuture<Breakpoint> resolveBreakpoint(@NotNull final String id) {
    if (myState == null) {
      return null;
    }
    final Debugger client = CloudDebuggerClient.getCloudDebuggerClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to resolveBreakpointAsync");
      Messages.showErrorDialog(myState.getProject(), GctBundle.getString("clouddebug.bad_login_message"),
                               GctBundle.getString("clouddebug.errortitle"));
      return null;
    }
    List<Breakpoint> currentList = myState.getCurrentServerBreakpointList();
    final SettableFuture<Breakpoint> future = SettableFuture.create();
    final Ref<Breakpoint> resultingBreakpointRef = new Ref<Breakpoint>();
    for (Breakpoint serverBreakpointCandidate : currentList) {
      if (serverBreakpointCandidate.getId().equals(id)) {
        resultingBreakpointRef.set(serverBreakpointCandidate);
        break;
      }
    }

    if (!resultingBreakpointRef.isNull()) {
      // If our breakpoint isn't final, they we do not need extra information and
      // can return the result immediately.
      if (resultingBreakpointRef.get().getIsFinalState() != Boolean.TRUE) {
        future.set(resultingBreakpointRef.get());
        return future;
      }

      ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
        @Override
        public void run() {
          //At this point, the user has selected a final state breakpoint which is not yet hydrated.
          //So we query the server to get this final on a worker thread and then run the runnable
          // back on ui
          GetBreakpointResponse response;
          try {
            response = client.debuggees().breakpoints().get(myState.getDebuggeeId(), id).execute();
            Breakpoint result = response.getBreakpoint();
            if (result != null) {
              resultingBreakpointRef.set(result);
              myFullFinalBreakpoints.put(id, result);
              future.set(resultingBreakpointRef.get());
            }
          }
          catch (IOException e) {
            LOG.error("IOException hydrating a final snapshot ", e);
            future.setException(e);
          }
        }
      });
      return future;
    }
    LOG.warn("could not resolve breakpoint " + id);

    return null;
  }

  /**
   * Called from the {@link CloudDebugProcessHandler} to set a breakpoint.
   *
   * @param serverBreakpoint the breakpoint being added
   * @param errorHandler     the handler that gets called if an error occurs during the add call
   * @return the ID of the newly added breakpoint, if successful
   */
  String setBreakpoint(@NotNull Breakpoint serverBreakpoint, @Nullable BreakpointErrorHandler errorHandler) {
    if (myState == null) {
      return null;
    }
    final Debugger client = CloudDebuggerClient.getCloudDebuggerClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      Messages.showErrorDialog(myState.getProject(), GctBundle.getString("clouddebug.bad_login_message"),
                               GctBundle.getString("clouddebug.errortitle"));
      return null;
    }

    try {
      // Delete old breakpoints at this location.
      List<Breakpoint> currentList = myState.getCurrentServerBreakpointList();
      SourceLocation location = serverBreakpoint.getLocation();
      for (Breakpoint serverBp : currentList) {
        if (serverBp.getIsFinalState() != Boolean.TRUE &&
            serverBp.getLocation().getLine() != null &&
            serverBp.getLocation().getLine().equals(location.getLine()) &&
            !Strings.isNullOrEmpty(serverBp.getLocation().getPath()) &&
            serverBp.getLocation().getPath().equals(location.getPath())) {
          deleteBreakpoint(serverBp.getId());
        }
      }

      SetBreakpointResponse addResponse =
        client.debuggees().breakpoints().set(myState.getDebuggeeId(), serverBreakpoint).execute();

      if (addResponse != null && addResponse.getBreakpoint() != null) {
        Breakpoint result = addResponse.getBreakpoint();
        if (result.getStatus() != null &&
            result.getStatus().getIsError() == Boolean.TRUE &&
            errorHandler != null &&
            result.getStatus().getDescription() != null) {
          errorHandler.handleError(BreakpointUtil.getUserErrorMessage(result.getStatus()));
        }
        return addResponse.getBreakpoint().getId();
      }
    }
    catch (IOException ex) {
      LOG.error("exception setting a breakpoint", ex);
    }

    return null;
  }

  /**
   * Begins background listening from the server.  When changes occur, listeners are notified.
   */
  public void startBackgroundListening() {
    assert myState != null;
    if (myListBreakpointsJob == null) {
      myListBreakpointsJob = new Timer("list breakpoints");
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          waitForChanges();
          Timer timer = myListBreakpointsJob;
          if (timer != null) {
            try {
              // We run after a short period to act as a throttle.
              timer.schedule(new RunnableTimerTask(this), PERIOD_MS);
            }
            catch (IllegalStateException ex) {
              //This can happen in rare race conditions and isn't an error.  We just ignore it.
            }
          }
        }
      };
      myListBreakpointsJob.schedule(new RunnableTimerTask(runnable), INITIAL_DELAY_MS);
    }
  }

  /**
   * Stops background listening.
   */
  public void stopBackgroundListening() {
    if (myListBreakpointsJob != null) {
      myListBreakpointsJob.cancel();
    }
    myListBreakpointsJob = null;
  }

  /**
   * Package protected for test purposes only.
   */
  void waitForChanges() {
    if (myState == null) {
      LOG.error("no state available attempting to checkForChanges");
      return;
    }
    final Debugger client = CloudDebuggerClient.getCloudDebuggerClient(myState);
    if (client == null) {
      LOG.info("no client available attempting to checkForChanges");
      return;
    }

    String tokenToSend = myState.getWaitToken();
    List<Breakpoint> currentList;
    try {
      currentList = queryServerForBreakpoints(myState, client, tokenToSend);
    }
    catch (SocketTimeoutException ex) {
      // Timeout is expected on a hanging get.
      return;
    }
    catch (GoogleJsonResponseException ex) {
      // A 409 JsonResponseException is used by the server to indicate to us a change happened and
      // we need to requery.
      if (ex.getDetails().getCode() == 409) {
        try {
          currentList = queryServerForBreakpoints(myState, client, tokenToSend);
        }
        catch (IOException ioException) {
          LOG.warn("exception listing breakpoints", ioException);
          return;
        }
      }
      else {
        LOG.warn("exception listing breakpoints", ex);
        return;
      }
    }
    catch (IOException ex) {
      LOG.warn("exception listing breakpoints", ex);
      return;
    }

    //tokenToSend can be null on first initialization -- where we shouldn't fire events or need
    // to do pruning.
    if (!Strings.isNullOrEmpty(tokenToSend)) {
      pruneBreakpointCache(currentList);
      fireBreakpointsChanged();
    }
  }

  private static List<Breakpoint> queryServerForBreakpoints(CloudDebugProcessState state,
                                                            Debugger client,
                                                            String tokenToSend) throws IOException {
    List<Breakpoint> currentList;

    ListBreakpointsResponse response =
      client.debuggees().breakpoints().list(state.getDebuggeeId()).setIncludeInactive(Boolean.TRUE).setAction("CAPTURE")
        .setStripResults(Boolean.TRUE).setWaitToken(tokenToSend).execute();

    currentList = response.getBreakpoints();
    String responseWaitToken = response.getWaitToken();
    state.setWaitToken(responseWaitToken);

    if (currentList != null) {
      Collections.sort(currentList, BreakpointComparer.getDefaultInstance());
    }

    state.setCurrentServerBreakpointList(currentList != null
                                         ? ContainerUtil.immutableList(currentList)
                                         : ContainerUtil.immutableList(new ArrayList<Breakpoint>()));

    return currentList;
  }

  private void pruneBreakpointCache(List<Breakpoint> currentList) {
    //Clear out the obsolete breakpoint cache for old items.
    HashSet<String> toRemoveSet = new HashSet<String>();
    toRemoveSet.addAll(myFullFinalBreakpoints.keySet());
    if (!toRemoveSet.isEmpty() && currentList != null) {
      for (Breakpoint bp : currentList) {
        toRemoveSet.remove(bp.getId());
      }
    }

    for (String idToRemove : toRemoveSet) {
      myFullFinalBreakpoints.remove(idToRemove);
    }
  }

  public static interface BreakpointErrorHandler {
    void handleError(String errorMessage);
  }

  static class RunnableTimerTask extends TimerTask {
    private final Runnable myRunnable;

    public RunnableTimerTask(@NotNull Runnable runnable) {
      myRunnable = runnable;
    }

    @Override
    public void run() {
      myRunnable.run();
    }
  }
}
