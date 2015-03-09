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

import com.google.api.services.debugger.model.Breakpoint;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gct.idea.debugger.ui.CloudDebugHistoricalSnapshots;
import com.google.gct.idea.util.GctBundle;
import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutAttractionPolicy;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ComponentWithActions;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

import javax.swing.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * CloudDebugProcess is the controller that represents our attached state to the server. It provides the breakpoint
 * handler as well as functionality for stepover/into (which is disabled) Most importantly for the Cloud Debugger, it
 * customizes the UI layout and polls the server for changes.
 * <p/>
 * It also sets the debug session to a certain snapshot when appropriate which requires the creation of a {@link
 * CloudExecutionStack}.
 * <p/>
 * CloudDebugProcess only exists for the duration of the IDE debug session.
 * <p/>
 * It also contains within it state {@link CloudDebugProcessState} which can live beyond the lifetime of the debug
 * session and be serialized into workspace.xml state.
 */
public class CloudDebugProcess extends XDebugProcess implements CloudBreakpointListener {
  private static final Logger LOG = Logger.getInstance(CloudDebugProcess.class);
  private volatile Breakpoint myCurrentSnapshot;
  private CloudDebugProcessState myProcessState;
  private ProjectRepositoryValidator myRepoValidator;
  private CloudDebugProcessStateController myStateController;
  private XBreakpointHandler<?>[] myXBreakpointHandlers;

  CloudDebugProcess(@NotNull XDebugSession session) {
    super(session);
  }

  public void addListener(@NotNull CloudBreakpointListener listener) {
    getStateController().addListener(listener);
  }

  @Override
  public boolean checkCanPerformCommands() {
    Messages.showErrorDialog("The Cloud Debugger does not pause execution.  Therefore, this feature is unavailable.",
                             "Not Supported");
    return false;
  }

  @Override
  @NotNull
  public XDebugTabLayouter createTabLayouter() {
    final CloudDebugProcessHandler handler = (CloudDebugProcessHandler)getProcessHandler();

    return new XDebugTabLayouter() {
      @Override
      public void registerAdditionalContent(@NotNull RunnerLayoutUi layout) {
        layout.removeContent(layout.findContent(DebuggerContentInfo.WATCHES_CONTENT), false);

        Content content = layout.findContent(DebuggerContentInfo.FRAME_CONTENT);
        if (content != null) {
          layout.removeContent(content, false);
          layout.addContent(content, 0, PlaceInGrid.center, false);
        }

        content = layout.findContent(DebuggerContentInfo.VARIABLES_CONTENT);
        if (content != null) {
          layout.removeContent(content, false);
          layout.addContent(content, 0, PlaceInGrid.right, false);
        }

        CloudDebugHistoricalSnapshots timeline = new CloudDebugHistoricalSnapshots(handler);
        Content snapshots = layout
          .createContent(timeline.getTabTitle(), (ComponentWithActions)timeline, timeline.getTabTitle(),
                         GoogleCloudToolsIcons.CLOUD, null);
        layout.addContent(snapshots, 0, PlaceInGrid.left, false);

        layout.getDefaults().initFocusContent(DebuggerContentInfo.FRAME_CONTENT, LayoutViewOptions.STARTUP,
                                              new LayoutAttractionPolicy.FocusOnce(false));
      }
    };
  }

  @Nullable
  @Override
  protected ProcessHandler doGetProcessHandler() {
    return new CloudDebugProcessHandler(this);
  }

  public void fireBreakpointsChanged() {
    getStateController().fireBreakpointsChanged();
  }

  @NotNull
  public CloudBreakpointHandler getBreakpointHandler() {
    return (CloudBreakpointHandler)getBreakpointHandlers()[0];
  }

  @NotNull
  @Override
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    if (myXBreakpointHandlers == null) {
      myXBreakpointHandlers = new XBreakpointHandler<?>[]{new CloudBreakpointHandler(this)};
    }
    return myXBreakpointHandlers;
  }

  /**
   * The value returned from the method is immutable and is safe to access on multiple threads.
   * <p/>
   * However, multiple successive calls to this method may return a different list. Therefore, callers must store the
   * return value locally to operate on it and should not call this method repeatedly expecting the same list.
   */
  public List<Breakpoint> getCurrentBreakpointList() {
    return getProcessState().getCurrentServerBreakpointList();
  }

  /**
   * Returns the breakpoint (snapshot) that the debug session is currently analyzing.
   */
  public Breakpoint getCurrentSnapshot() {
    return myCurrentSnapshot;
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new JavaDebuggerEditorsProvider();
  }

  public CloudDebugProcessState getProcessState() {
    return myProcessState;
  }

  protected ProjectRepositoryValidator getRepositoryValidator() {
    if (myRepoValidator == null) {
      myRepoValidator = new ProjectRepositoryValidator(getProcessState());
    }
    return myRepoValidator;
  }

  public CloudDebugProcessStateController getStateController() {
    if (myStateController == null) {
      myStateController = new CloudDebugProcessStateController();
    }
    return myStateController;
  }

  public XDebugSession getXDebugSession() {
    return getSession();
  }

  /**
   * Initializes the current state, synchronously checks for the latest set of changes and kicks off the background job
   * to poll for changes.
   */
  public void initialize(@NotNull CloudDebugProcessState processState) {
    myProcessState = processState;
    myCurrentSnapshot = null;

    new Task.Modal(getXDebugSession().getProject(), GctBundle.getString("clouddebug.attachingtext"), false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        getStateController().initialize(myProcessState);
        getRepositoryValidator().hardRefresh();
      }
    }.queue();

    JavaUtil.initializeLocations(getXDebugSession().getProject(), true);
    // Start breakpoints refresh job on first use.
    getStateController().addListener(this);
    getStateController().startBackgroundListening();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        getBreakpointHandler().createIdeRepresentationsIfNecessary(getCurrentBreakpointList());
      }
    });
  }

  /**
   * Finds the snapshot associated with the given id and sets it as the active snapshot in the current debug session.
   */
  public void navigateToSnapshot(@NotNull String id) {
    ListenableFuture<Breakpoint> future = getStateController().resolveBreakpoint(id);
    if (future != null) {
      Futures.addCallback(future, new FutureCallback<Breakpoint>() {
        @Override
        public void onSuccess(final Breakpoint result) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              if (result.getIsFinalState() != Boolean.TRUE || result.getStackFrames() == null) {
                getBreakpointHandler().navigateTo(result);
                return;
              }

              if (myCurrentSnapshot == null || !myCurrentSnapshot.getId().equals(result.getId())) {
                Date brokenTime = new Date(result.getFinalTime().getSeconds() * 1000);
                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                myCurrentSnapshot = result;
                if (!getXDebugSession().isStopped()) {
                  getXDebugSession().positionReached(new MySuspendContext(
                    new CloudExecutionStack(getXDebugSession().getProject(),
                                            GctBundle.getString("clouddebug.stackat", df.format(brokenTime)),
                                            result.getStackFrames(), result.getVariableTable(),
                                            result.getEvaluatedExpressions())));
                }
              }
            }
          });
        }

        @Override
        public void onFailure(@NotNull Throwable t) {
          LOG.warn("Could not navigate to breakpoint:", t);
        }
      });
    }
  }

  /**
   * Called when the poll job detects a change in the list of breakpoints. It will disable the ide breakpoint and move
   * the debug session to that snapshot if nothing has yet been selected.
   */
  @Override
  public void onBreakpointListChanged(CloudDebugProcessState state) {
    // We always snap the current breakpoint list before working on it.
    final List<Breakpoint> currentList = getCurrentBreakpointList();
    if (currentList != null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          getBreakpointHandler().createIdeRepresentationsIfNecessary(currentList);
        }
      });
      for (Breakpoint breakpoint : currentList) {
        final XBreakpoint breakpointHit = getBreakpointHandler().getEnabledXBreakpoint(breakpoint);
        if (breakpointHit == null) {
          continue;
        }

        if (breakpoint.getIsFinalState() == Boolean.TRUE &&
            (breakpoint.getStatus() == null || breakpoint.getStatus().getIsError() != Boolean.TRUE)) {
          //if we have a final state breakpoint, we disable the ide breakpoint
          // and if the user hasn't selected a snapshot, we navigate to the first snapshot
          // received during the session.
          if (myCurrentSnapshot == null) {
            ListenableFuture<Breakpoint> future = getStateController().resolveBreakpoint(breakpoint.getId());
            if (future != null) {
              Futures.addCallback(future, new FutureCallback<Breakpoint>() {
                @Override
                public void onSuccess(final Breakpoint result) {
                  SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                      //time to break!
                      Date brokenTime = new Date(result.getFinalTime().getSeconds() * 1000);
                      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                      myCurrentSnapshot = result;

                      if (!getXDebugSession().isStopped()) {
                        getXDebugSession().positionReached(new MySuspendContext(
                          new CloudExecutionStack(getXDebugSession().getProject(),
                                                  GctBundle.getString("clouddebug.stackat", df.format(brokenTime)),
                                                  result.getStackFrames(), result.getVariableTable(),
                                                  result.getEvaluatedExpressions())));
                      }
                    }
                  });
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                  LOG.warn("was unable to hydrate breakpoint on ListChanged", t);
                }

              });
            }
          }
          if (!getXDebugSession().isStopped()) {
            getBreakpointHandler().setStateToDisabled(breakpoint);
          }
        }
        else if (breakpoint.getIsFinalState() == Boolean.TRUE) {
          // then this is an error state breakpoint.
          com.intellij.debugger.ui.breakpoints.Breakpoint cloudBreakpoint =
            BreakpointManager.getJavaBreakpoint(breakpointHit);
          if (breakpoint.getStatus() != null &&
              breakpoint.getStatus().getIsError() == Boolean.TRUE &&
              cloudBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
            ((CloudLineBreakpointType.CloudLineBreakpoint)cloudBreakpoint)
              .setErrorMessage(BreakpointUtil.getUserErrorMessage(breakpoint.getStatus()));
            cloudBreakpoint.updateUI();
          }
        }
      }
    }
  }

  @Override
  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar,
                                        @NotNull DefaultActionGroup topToolbar,
                                        @NotNull DefaultActionGroup settings) {
    ActionManager manager = ActionManager.getInstance();
    leftToolbar.add(new SaveAndExitAction(), new Constraints(Anchor.AFTER, IdeActions.ACTION_STOP_PROGRAM));

    leftToolbar.remove(manager.getAction(IdeActions.ACTION_RERUN));
    leftToolbar.remove(manager.getAction(IdeActions.ACTION_STOP_PROGRAM));
    leftToolbar.remove(manager.getAction(XDebuggerActions.RESUME));
    leftToolbar.remove(manager.getAction(XDebuggerActions.PAUSE));
    leftToolbar.remove(manager.getAction(XDebuggerActions.MUTE_BREAKPOINTS));

    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_OVER));
    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_INTO));
    topToolbar.remove(manager.getAction(XDebuggerActions.FORCE_STEP_INTO));
    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_OUT));
    topToolbar.remove(manager.getAction(XDebuggerActions.RUN_TO_CURSOR));
    topToolbar.remove(manager.getAction(XDebuggerActions.EVALUATE_EXPRESSION));
  }

  public void removeListener(@NotNull CloudBreakpointListener listener) {
    getStateController().removeListener(listener);
  }

  @Override
  public void resume() {
  }

  @Override
  public void runToPosition(@NotNull XSourcePosition position) {
  }

  @Override
  public void startStepInto() {
  }

  @Override
  public void startStepOut() {
  }

  @Override
  public void startStepOver() {
  }

  @Override
  public void stop() {
    getStateController().stopBackgroundListening();

    RunProfile profile = getXDebugSession().getRunProfile();
    if (profile instanceof CloudDebugRunConfiguration) {
      ((CloudDebugRunConfiguration)profile).setProcessState(myProcessState);
    }

    getRepositoryValidator().restoreToOriginalState(getXDebugSession().getProject());

    XBreakpointManager breakpointManager =
      XDebuggerManager.getInstance(getXDebugSession().getProject()).getBreakpointManager();
    for (XBreakpoint bp : breakpointManager.getAllBreakpoints()) {
      com.intellij.debugger.ui.breakpoints.Breakpoint cloudBreakpoint = BreakpointManager.getJavaBreakpoint(bp);
      if (!(cloudBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint)) {
        continue;
      }
      CloudLineBreakpointType.CloudLineBreakpoint cloudLineBreakpoint =
        (CloudLineBreakpointType.CloudLineBreakpoint)cloudBreakpoint;
      cloudLineBreakpoint.setVerified(false);
      cloudLineBreakpoint.setErrorMessage(null);
      cloudLineBreakpoint.updateUI();
    }
  }

  // These are used to hide unsupported actions.
  static interface XDebuggerActions {
    @NonNls String EVALUATE_EXPRESSION = "EvaluateExpression";
    @NonNls String FORCE_STEP_INTO = "ForceStepInto";
    @NonNls String MUTE_BREAKPOINTS = "XDebugger.MuteBreakpoints";
    @NonNls String PAUSE = "Pause";
    @NonNls String RESUME = "Resume";
    @NonNls String RUN_TO_CURSOR = "RunToCursor";
    @NonNls String STEP_INTO = "StepInto";
    @NonNls String STEP_OUT = "StepOut";
    @NonNls String STEP_OVER = "StepOver";
  }

  /**
   * The suspend context gives the debug session the information necessary to populate the stack and variables windows.
   */
  private static class MySuspendContext extends XSuspendContext {
    private final XExecutionStack myStack;

    public MySuspendContext(@NotNull XExecutionStack stack) {
      myStack = stack;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
      return myStack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
      return new XExecutionStack[]{getActiveExecutionStack(), getSourceStack()};
    }

    @NotNull
    public XExecutionStack getSourceStack() {
      return myStack;
    }
  }

  private class SaveAndExitAction extends AnAction {
    public SaveAndExitAction() {
      super(GctBundle.getString("clouddebug.stopandcontinue"), GctBundle.getString("clouddebug.exitdebug"),
            GoogleCloudToolsIcons.CLOUD_DEBUG_SAVE_EXIT);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM).actionPerformed(e);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(!CloudDebugProcess.this.getXDebugSession().isStopped());
    }
  }
}
