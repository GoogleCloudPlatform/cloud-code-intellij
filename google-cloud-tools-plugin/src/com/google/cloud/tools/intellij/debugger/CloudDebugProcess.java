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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessStateController.ResolveBreakpointHandler;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.cloud.tools.intellij.debugger.actions.CloudDebugHelpAction;
import com.google.cloud.tools.intellij.debugger.ui.CloudDebugHistoricalSnapshots;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.util.GctTracking;

import com.intellij.debugger.actions.DebuggerActions;
import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutAttractionPolicy;
import com.intellij.execution.ui.layout.LayoutViewOptions;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Anchor;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
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
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.ui.XDebugTabLayouter;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.JavaDebuggerEditorsProvider;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * CloudDebugProcess is the controller that represents our attached state to the server. It provides
 * the breakpoint handler as well as functionality for stepover/into (which is disabled) Most
 * importantly for the Cloud Debugger, it customizes the UI layout and polls the server for
 * changes.
 * <p/>
 * It also sets the debug session to a certain snapshot when appropriate which requires the creation
 * of a {@link CloudExecutionStack}.
 * <p/>
 * CloudDebugProcess only exists for the duration of the IDE debug session.
 * <p/>
 * It also contains state {@link CloudDebugProcessState} that can live beyond the lifetime of the
 * debug session and be serialized into workspace.xml state.
 */
public class CloudDebugProcess extends XDebugProcess implements CloudBreakpointListener {

  private static final Logger LOG = Logger.getInstance(CloudDebugProcess.class);
  private volatile Breakpoint currentSnapshot;
  private CloudDebugProcessState processState;
  private ProjectRepositoryValidator repoValidator;
  private CloudDebugProcessStateController stateController;
  private XBreakpointHandler<?>[] breakpointHandlers;
  private volatile String navigatedSnapshotId;

  public CloudDebugProcess(@NotNull XDebugSession session) {
    super(session);
  }

  public void addListener(@NotNull CloudBreakpointListener listener) {
    getStateController().addListener(listener);
  }

  @Override
  public boolean checkCanPerformCommands() {
    Messages.showErrorDialog(
        "The Cloud Debugger does not pause execution. Therefore, this feature is unavailable.",
        "Not Supported");
    return false;
  }

  @Override
  @NotNull
  public XDebugTabLayouter createTabLayouter() {
    final CloudDebugProcessHandler handler = (CloudDebugProcessHandler) getProcessHandler();

    return new XDebugTabLayouter() {
      @Override
      public void registerAdditionalContent(@NotNull RunnerLayoutUi layout) {
        layout.removeContent(layout.findContent(DebuggerContentInfo.WATCHES_CONTENT), false);

        // remove console since the cloud debugger doesn't use it
        // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/141
        Content consoleContent = layout.findContent(DebuggerContentInfo.CONSOLE_CONTENT);
        if (consoleContent != null) {
          layout.removeContent(consoleContent, false);
        }

        Content frameContent = layout.findContent(DebuggerContentInfo.FRAME_CONTENT);
        if (frameContent != null) {
          layout.removeContent(frameContent, false);
          layout.addContent(frameContent, 0, PlaceInGrid.center, false);
        }

        Content variablesContent = layout.findContent(DebuggerContentInfo.VARIABLES_CONTENT);
        if (variablesContent != null) {
          layout.removeContent(variablesContent, false);
          layout.addContent(variablesContent, 0, PlaceInGrid.right, false);
        }

        CloudDebugHistoricalSnapshots timeline = new CloudDebugHistoricalSnapshots(handler);
        timeline.onBreakpointListChanged(getProcessState());
        Content snapshots = layout
            .createContent(timeline.getTabTitle(), (ComponentWithActions) timeline,
                timeline.getTabTitle(),
                GoogleCloudToolsIcons.APP_ENGINE, null);
        layout.addContent(snapshots, 0, PlaceInGrid.left, false);

        layout.getDefaults().initFocusContent(timeline.getTabTitle(), LayoutViewOptions.STARTUP,
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
    return (CloudBreakpointHandler) getBreakpointHandlers()[0];
  }

  @NotNull
  @Override
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    if (breakpointHandlers == null) {
      breakpointHandlers = new XBreakpointHandler<?>[]{
          new CloudBreakpointHandler(this, new ServerToIdeFileResolver())
      };
    }
    return breakpointHandlers;
  }

  @VisibleForTesting
  void setBreakpointHandler(CloudBreakpointHandler handler) {
    breakpointHandlers = new XBreakpointHandler<?>[]{handler};
  }

  /**
   * The value returned from the method is immutable and is safe to access on multiple threads.
   * <p/>
   * However, multiple successive calls to this method may return a different list. Therefore,
   * callers must store the return value locally to operate on it and should not call this method
   * repeatedly expecting the same list.
   */
  // todo: can we declare this as ImmutableList?
  public List<Breakpoint> getCurrentBreakpointList() {
    return getProcessState().getCurrentServerBreakpointList();
  }

  /**
   * Returns the breakpoint (snapshot) that the debug session is currently analyzing.
   */
  public Breakpoint getCurrentSnapshot() {
    return currentSnapshot;
  }

  @NotNull
  @Override
  public XDebuggerEditorsProvider getEditorsProvider() {
    return new JavaDebuggerEditorsProvider();
  }

  public CloudDebugProcessState getProcessState() {
    return processState;
  }

  protected ProjectRepositoryValidator getRepositoryValidator() {
    if (repoValidator == null) {
      repoValidator = new ProjectRepositoryValidator(getProcessState());
    }
    return repoValidator;
  }

  /**
   * Return the state controller creating a new one if needed.
   */
  public CloudDebugProcessStateController getStateController() {
    if (stateController == null) {
      stateController = new CloudDebugProcessStateController();
    }
    return stateController;
  }

  public XDebugSession getXDebugSession() {
    return getSession();
  }

  /**
   * Initializes the current state, synchronously checks for the latest set of changes and kicks off
   * the background job to poll for changes.
   */
  public void initialize(@NotNull CloudDebugProcessState processState) {
    this.processState = processState;
    currentSnapshot = null;

    new Task.Modal(getXDebugSession().getProject(), GctBundle.getString("clouddebug.attachingtext"),
        false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        getStateController().initialize(CloudDebugProcess.this.processState);
        getRepositoryValidator().hardRefresh();
      }
    }.queue();

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
   * Clears out the current execution stack while keeping the debug session alive. Useful for
   * breakpoint delete operations which result in no selected snapshots, requiring us to display an
   * empty stack in the UI, while keeping the debug session alive.
   */
  public void clearExecutionStack() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (!getXDebugSession().isStopped()) {
          // Since there is no equivalent metaphor in traditional debug sessions, this simulates
          // the desired behavior of clearing the current context by setting the current position
          // to an empty context
          getXDebugSession().positionReached(new XSuspendContext() {
          });
        }
      }
    });
  }

  /**
   * Finds the snapshot associated with the given id and sets it as the active snapshot in the
   * current debug session.
   */
  public void navigateToSnapshot(@NotNull final String id) {
    if (Strings.isNullOrEmpty(id)) {
      LOG.error("unexpected navigation to empty breakpoint id");
      return;
    }
    navigatedSnapshotId = id;
    getStateController().resolveBreakpointAsync(id,
        new ResolveBreakpointHandler() {
          @Override
          public void onSuccess(@NotNull final Breakpoint result) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                // We will only do the selection if the id for this async task matches the latest
                // user clicked item.  This prevents multiple (and possibly out of order)
                // selections getting queued up.
                if (id.equals(navigatedSnapshotId)) {
                  if (!Boolean.TRUE.equals(result.getIsFinalState())
                      || result.getStackFrames() == null) {
                    getBreakpointHandler().navigateTo(result);
                    if (result.getStackFrames() == null) {
                      navigateToBreakpoint(result);
                    }
                    return;
                  }

                  navigateToBreakpoint(result);
                }
              }
            });
          }

          @Override
          public void onError(String errorMessage) {
            LOG.warn("Could not navigate to breakpoint:" + errorMessage);
          }
        });
  }

  private void navigateToBreakpoint(@NotNull Breakpoint target) {
    Date snapshotTime = BreakpointUtil.parseDateTime(target.getFinalTime());
    if (snapshotTime == null) {
      snapshotTime = new Date();
    }
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    currentSnapshot = target;
    if (!getXDebugSession().isStopped()) {
      getXDebugSession().positionReached(new MySuspendContext(
          new CloudExecutionStack(getXDebugSession().getProject(),
              GctBundle.getString("clouddebug.stackat", df.format(snapshotTime)),
              target.getStackFrames(), target.getVariableTable(),
              target.getEvaluatedExpressions())));
    }
  }

  /**
   * Called when the poll job detects a change in the list of breakpoints. It will disable the ide
   * breakpoint and move the debug session to that snapshot if nothing has yet been selected.
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

        if (Boolean.TRUE.equals(breakpoint.getIsFinalState())
            && (breakpoint.getStatus() == null || !Boolean.TRUE
                .equals(breakpoint.getStatus().getIsError()))) {
          if (!getXDebugSession().isStopped()) {
            getBreakpointHandler().setStateToDisabled(breakpoint);
          }
        } else if (Boolean.TRUE.equals(breakpoint.getIsFinalState())) {
          // then this is an error state breakpoint.
          com.intellij.debugger.ui.breakpoints.Breakpoint cloudBreakpoint =
              BreakpointManager.getJavaBreakpoint(breakpointHit);
          if (breakpoint.getStatus() != null
              && Boolean.TRUE.equals(breakpoint.getStatus().getIsError())
              && cloudBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
            CloudLineBreakpoint cloudLineBreakpoint = (CloudLineBreakpoint) cloudBreakpoint;
            cloudLineBreakpoint
                .setErrorMessage(BreakpointUtil.getUserErrorMessage(breakpoint.getStatus()));
            updateBreakpointPresentation(cloudLineBreakpoint);
          }
        }
      }
    }
  }

  void updateBreakpointPresentation(CloudLineBreakpoint cloudLineBreakpoint) {
    final XBreakpointManager manager = XDebuggerManager
        .getInstance(getXDebugSession().getProject()).getBreakpointManager();
    manager.updateBreakpointPresentation(
        (XLineBreakpoint<?>) cloudLineBreakpoint.getXBreakpoint(),
        cloudLineBreakpoint.getSetIcon(areBreakpointsMuted()),
        cloudLineBreakpoint.getErrorMessage());
  }

  private boolean areBreakpointsMuted() {
    return getXDebugSession() != null && getXDebugSession().areBreakpointsMuted();
  }

  @Override
  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar,
      @NotNull DefaultActionGroup topToolbar,
      @NotNull DefaultActionGroup settings) {
    ActionManager manager = ActionManager.getInstance();
    leftToolbar.add(new SaveAndExitAction(),
        new Constraints(Anchor.AFTER, IdeActions.ACTION_STOP_PROGRAM));

    leftToolbar.remove(manager.getAction(IdeActions.ACTION_RERUN));
    leftToolbar.remove(manager.getAction(IdeActions.ACTION_STOP_PROGRAM));

    // XDebugSessionTab puts this action second from end.
    AnAction[] actions = leftToolbar.getChildActionsOrStubs();
    for (AnAction action : actions) {
      String text = action.getTemplatePresentation().getText();
      if (ExecutionBundle.message("close.tab.action.name").equals(text)) {
        leftToolbar.remove(action);
        break;
      }
    }

    // remove help button since it points to the IntelliJ help by default and we don't have
    // a help page yet.
    // for some reason, the help button's key in leftToolbar is null, so we need to remove it
    // by class name.
    // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/149
    for (AnAction child : leftToolbar.getChildActionsOrStubs()) {
      if (child.getClass().getCanonicalName().equalsIgnoreCase(
          "com.intellij.ide.actions.ContextHelpAction")) {
        // we never want to show IDEA's help.
        leftToolbar.remove(child);

        // show our help if we have it.
        String helpUrl = GctBundle.getString("clouddebug.helpurl");
        if (!"".equals(helpUrl)) {
          leftToolbar.add(new CloudDebugHelpAction(helpUrl));
        }
        break;
      }
    }

    leftToolbar.remove(manager.getAction(XDebuggerActions.RESUME));
    leftToolbar.remove(manager.getAction(XDebuggerActions.PAUSE));
    leftToolbar.remove(manager.getAction(XDebuggerActions.MUTE_BREAKPOINTS));

    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_OVER));
    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_INTO));
    topToolbar.remove(manager.getAction(XDebuggerActions.FORCE_STEP_INTO));
    topToolbar.remove(manager.getAction(XDebuggerActions.STEP_OUT));
    topToolbar.remove(manager.getAction(XDebuggerActions.RUN_TO_CURSOR));
    topToolbar.remove(manager.getAction(XDebuggerActions.EVALUATE_EXPRESSION));
    topToolbar.remove(manager.getAction(DebuggerActions.POP_FRAME));
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
      ((CloudDebugRunConfiguration) profile).setProcessState(processState);
    }

    getRepositoryValidator().restoreToOriginalState(getXDebugSession().getProject());

    XBreakpointManager breakpointManager =
        XDebuggerManager.getInstance(getXDebugSession().getProject()).getBreakpointManager();
    for (XBreakpoint bp : breakpointManager.getAllBreakpoints()) {
      com.intellij.debugger.ui.breakpoints.Breakpoint cloudBreakpoint = BreakpointManager
          .getJavaBreakpoint(bp);
      if (!(cloudBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint)) {
        continue;
      }
      CloudLineBreakpointType.CloudLineBreakpoint cloudLineBreakpoint =
          (CloudLineBreakpointType.CloudLineBreakpoint) cloudBreakpoint;
      cloudLineBreakpoint.setVerified(false);
      cloudLineBreakpoint.setErrorMessage(null);
      updateBreakpointPresentation(cloudLineBreakpoint);
    }
  }

  // These are used to hide unsupported actions.
  interface XDebuggerActions {

    @NonNls
    String EVALUATE_EXPRESSION = "EvaluateExpression";
    @NonNls
    String FORCE_STEP_INTO = "ForceStepInto";
    @NonNls
    String MUTE_BREAKPOINTS = "XDebugger.MuteBreakpoints";
    @NonNls
    String PAUSE = "Pause";
    @NonNls
    String RESUME = "Resume";
    @NonNls
    String RUN_TO_CURSOR = "RunToCursor";
    @NonNls
    String STEP_INTO = "StepInto";
    @NonNls
    String STEP_OUT = "StepOut";
    @NonNls
    String STEP_OVER = "StepOver";
  }

  /**
   * The suspend context gives the debug session the information necessary to populate the stack and
   * variables windows.
   */
  private static class MySuspendContext extends XSuspendContext {

    private final XExecutionStack stack;

    public MySuspendContext(@NotNull XExecutionStack stack) {
      this.stack = stack;
    }

    @Override
    public XExecutionStack getActiveExecutionStack() {
      return stack;
    }

    @Override
    public XExecutionStack[] getExecutionStacks() {
      return new XExecutionStack[]{getActiveExecutionStack(), getSourceStack()};
    }

    @NotNull
    public XExecutionStack getSourceStack() {
      return stack;
    }
  }

  private class SaveAndExitAction extends AnAction {

    public SaveAndExitAction() {
      super(GctBundle.getString("clouddebug.stopandcontinue"),
          GctBundle.getString("clouddebug.exitdebug"),
          GoogleCloudToolsIcons.CLOUD_DEBUG_SAVE_EXIT);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
      int result = Messages.showOkCancelDialog(event.getProject(),
          GctBundle.getString("clouddebug.continue.listening"),
          GctBundle.getString("clouddebug.message.title"),
          GctBundle.getString("clouddebug.continue"),
          GctBundle.getString("clouddebug.stop.listening"),
          Messages.getQuestionIcon());
      if (result == Messages.OK) { // continue
        processState.setListenInBackground(true);
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.CLOUD_DEBUGGER_CLOSE_CONTINUE_LISTEN)
            .ping();
      } else {
        processState.setListenInBackground(false);
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.CLOUD_DEBUGGER_CLOSE_STOP_LISTEN).ping();
      }
      ActionManager.getInstance().getAction(IdeActions.ACTION_STOP_PROGRAM).actionPerformed(event);
      ActionManager.getInstance().getAction(IdeActions.ACTION_CLOSE).actionPerformed(event);
    }

    @Override
    public void update(AnActionEvent event) {
      event.getPresentation().setEnabled(!CloudDebugProcess.this.getXDebugSession().isStopped());
    }
  }
}
