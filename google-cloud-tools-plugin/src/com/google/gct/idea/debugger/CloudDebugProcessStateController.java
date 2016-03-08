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
import com.google.api.services.clouddebugger.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.model.*;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gct.idea.util.GctBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;

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
  private volatile Timer myListBreakpointsJob;
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

  void deleteBreakpoint(@NotNull final String breakpointId) {
    deleteBreakpoint(breakpointId, false);
  }

  void deleteBreakpointAsync(@NotNull final String breakpointId) {
    deleteBreakpoint(breakpointId, true);
  }

  /**
   * Called from the {@link CloudBreakpointHandler} to remove breakpoints from the server.
   *
   * @param breakpointId the {@link Breakpoint} Id to delete
   */
  private void deleteBreakpoint(@NotNull final String breakpointId, boolean performAsync) {
    if (myState == null) {
      throw new IllegalStateException();
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      Messages.showErrorDialog(myState.getProject(), GctBundle.getString("clouddebug.bad.login.message"),
                               GctBundle.getString("clouddebug.message.title"));
      return;
    }
    final String debuggeeId = myState.getDebuggeeId();
    assert debuggeeId != null;

    Runnable performDelete = new Runnable() {
      @Override
      @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
      public void run() {
        try {
          client.debuggees().breakpoints().delete(debuggeeId, breakpointId).execute();
        } catch (IOException ex) {
          LOG.warn("exception deleting breakpoint " + breakpointId, ex);
        }
      }
    };

    if (performAsync) {
      ApplicationManager.getApplication().executeOnPooledThread(performDelete);
    }
    else {
      performDelete.run();
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
   */
  public void resolveBreakpointAsync(@NotNull final String id,
      @NotNull final ResolveBreakpointHandler handler) {

    if (myFullFinalBreakpoints.containsKey(id)) {
      handler.onSuccess(myFullFinalBreakpoints.get(id));
      return;
    }
    if (myState == null) {
      handler.onError(GctBundle.getString("clouddebug.invalid.state"));
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to resolveBreakpointAsync");
      handler.onError(GctBundle.getString("clouddebug.bad.login.message"));
      return;
    }
    List<Breakpoint> currentList = myState.getCurrentServerBreakpointList();
    final SettableFuture<Breakpoint> future = SettableFuture.create();
    for (Breakpoint serverBreakpointCandidate : currentList) {
      if (serverBreakpointCandidate.getId().equals(id)
          && serverBreakpointCandidate.getIsFinalState() != Boolean.TRUE) {
        handler.onSuccess(serverBreakpointCandidate);
        return;
      }
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
            myFullFinalBreakpoints.put(id, result);
            handler.onSuccess(result);
          }
          else {
            handler.onError(GctBundle.getString("clouddebug.no.response"));
          }
        }
        catch (IOException e) {
          LOG.warn("IOException hydrating a snapshot.  User may have deleted the snapshot", e);
          handler.onError(e.toString());
        }
      }
    });
  }

  /**
   * Called from the {@link CloudDebugProcessHandler} to set a breakpoint.
   */
  void setBreakpointAsync(@NotNull final Breakpoint serverBreakpoint,
      @NotNull final SetBreakpointHandler handler) {

    if (myState == null) {
      handler.onError(GctBundle.getString("clouddebug.invalid.state"));
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(myState);
    if (client == null) {
      LOG.warn("no client available attempting to setBreakpoint");
      handler.onError(GctBundle.getString("clouddebug.bad.login.message"));
      return;
    }

    final String debuggeeId = myState.getDebuggeeId();
    assert debuggeeId != null;

    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
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
              deleteBreakpoint(serverBp.getId()); //should not be async here.
            }
          }

          SetBreakpointResponse addResponse =
              client.debuggees().breakpoints().set(debuggeeId, serverBreakpoint).execute();

          if (addResponse != null && addResponse.getBreakpoint() != null) {
            Breakpoint result = addResponse.getBreakpoint();
            if (result.getStatus() != null &&
                result.getStatus().getIsError() == Boolean.TRUE &&
                handler != null &&
                result.getStatus().getDescription() != null) {
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
      }
    });
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

  boolean isBackgroundListening() {
    return myListBreakpointsJob != null;
  }

  /**
   * Package protected for test purposes only.
   */
  void waitForChanges() {
    if (myState == null) {
      LOG.error("no state available attempting to checkForChanges");
      return;
    }
    final Debugger client = CloudDebuggerClient.getLongTimeoutClient(myState);
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

  private List<Breakpoint> queryServerForBreakpoints(CloudDebugProcessState state,
                                                            Debugger client,
                                                            String tokenToSend) throws IOException {
    List<Breakpoint> currentList = null;

    String responseWaitToken = tokenToSend;

    while(tokenToSend == null || tokenToSend.equals(responseWaitToken)) {
      if (tokenToSend != null && !isBackgroundListening()) {
        return null;
      }

      ListBreakpointsResponse response =
          client.debuggees().breakpoints().list(state.getDebuggeeId())
              .setIncludeInactive(Boolean.TRUE).setActionValue("CAPTURE")
              .setStripResults(Boolean.TRUE)
              .setWaitToken(CloudDebugConfigType.useWaitToken() ? tokenToSend : null).execute();

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

  interface SetBreakpointHandler {
    void onSuccess(@NotNull String newBreakpointId);
    void onError(String errorMessage);
  }

  interface ResolveBreakpointHandler {
    void onSuccess(@NotNull Breakpoint newBreakpoint);
    void onError(String errorMessage);
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
