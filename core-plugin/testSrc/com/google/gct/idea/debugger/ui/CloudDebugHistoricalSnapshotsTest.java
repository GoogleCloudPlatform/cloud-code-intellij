package com.google.gct.idea.debugger.ui;

import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.SourceLocation;
import com.google.gct.idea.debugger.CloudBreakpointHandler;
import com.google.gct.idea.debugger.CloudDebugProcess;
import com.google.gct.idea.debugger.CloudDebugProcessHandler;
import com.google.gct.idea.debugger.CloudDebugProcessState;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.mock.MockApplication;
import com.intellij.mock.MockApplicationEx;
import com.intellij.mock.MockProject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.TimerListener;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.switcher.SwitchTarget;
import com.intellij.util.pico.DefaultPicoContainer;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.event.HyperlinkListener;


// TODO: This test hangs when run as part of the full suite.
// More generally we have a testing infrastructure problem. There's shared state across test
// suites that's causing unpredictable failures.
@Ignore
public class CloudDebugHistoricalSnapshotsTest {

  private CloudDebugHistoricalSnapshots snapshots;
  private Disposable parent = new MockDisposable();
  private CloudDebugProcess mockProcess = Mockito.mock(CloudDebugProcess.class);
  private CloudDebugProcessHandler handler = Mockito.mock(CloudDebugProcessHandler.class);

  @Before
  public void setUp() {
    MockApplication application = new MyMockApplicationEx(parent);
    ActionManager manager = new MockActionManager();
    application.addComponent(ActionManager.class, manager);
    ApplicationManager.setApplication(application, parent);

    XDebugSession session = new MockSession();
    Mockito.when(handler.getProcess()).thenReturn(mockProcess);
    Mockito.when(mockProcess.getXDebugSession()).thenReturn(session);

    snapshots = new CloudDebugHistoricalSnapshots(handler);
  }

  @After
  public void tearDown() {
    Disposer.dispose(parent);
  }

  @Test
  public void testOnBreakpointListChanged_noChanges() {
    CloudDebugProcessState state = new CloudDebugProcessState();
    snapshots.onBreakpointListChanged(state);

    Assert.assertNull(snapshots.myBalloon);
  }

  @Test
  public void testOnBreakpointListChanged() throws InterruptedException {
    CloudDebugProcessState state = new CloudDebugProcessState();

    Breakpoint bp1 = new Breakpoint();
    bp1.setId("an ID");
    bp1.setFinalTime("2015-08-22T05:23:34.123Z");
    bp1.setIsFinalState(true);
    SourceLocation location = new SourceLocation();
    location.setPath("foo/bar/baz");
    location.setLine(12);
    bp1.setLocation(location);

    List<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
    breakpoints.add(bp1);

    Mockito.when(mockProcess.getCurrentBreakpointList()).thenReturn(breakpoints);
    Mockito.when(mockProcess.getCurrentSnapshot()).thenReturn(bp1);
    CloudBreakpointHandler breakpointHandler = Mockito.mock(CloudBreakpointHandler.class);
    Mockito.when(mockProcess.getBreakpointHandler()).thenReturn(breakpointHandler);

    snapshots.onBreakpointListChanged(state);

    // wait for swing thread to run asynchronously; ugly and flaky;
    // is there a better way?
    Thread.sleep(1000);

    Assert.assertEquals(0, snapshots.myTable.getSelectedRow());
  }

  @Test
  public void testOnBreakpointListChanged_twoBreakPoints() throws InterruptedException {
    CloudDebugProcessState state = new CloudDebugProcessState();

    Breakpoint bp1 = new Breakpoint();
    bp1.setId("bp1");
    bp1.setFinalTime("2015-08-22T05:23:34.123Z");
    bp1.setIsFinalState(true);
    SourceLocation location = new SourceLocation();
    location.setPath("foo/bar/baz");
    location.setLine(12);
    bp1.setLocation(location);

    Breakpoint bp2 = new Breakpoint();
    bp2.setId("bp2");
    bp2.setFinalTime("2016-08-22T05:23:34.123Z");
    bp2.setIsFinalState(true);
    SourceLocation location2 = new SourceLocation();
    location2.setPath("foo/bar/baz");
    location2.setLine(14);
    bp2.setLocation(location);

    List<Breakpoint> breakpoints1 = new ArrayList<Breakpoint>();
    breakpoints1.add(bp1);

    List<Breakpoint> breakpoints2 = new ArrayList<Breakpoint>();
    breakpoints2.add(bp1);
    breakpoints2.add(bp2);

    Mockito.when(mockProcess.getCurrentBreakpointList()).thenReturn(breakpoints1, breakpoints2);
    Mockito.when(mockProcess.getCurrentSnapshot()).thenReturn(bp1, bp2);
    CloudBreakpointHandler breakpointHandler = Mockito.mock(CloudBreakpointHandler.class);
    Mockito.when(mockProcess.getBreakpointHandler()).thenReturn(breakpointHandler);

    Assert.assertEquals(-1, snapshots.myTable.getSelectedRow());

    snapshots.onBreakpointListChanged(state);

    // wait for swing thread to run asynchronously; ugly and flaky;
    // is there a better way?
    Thread.sleep(1000);
    Assert.assertEquals(0, snapshots.myTable.getSelectedRow());

    snapshots.onBreakpointListChanged(state);

    // wait for swing thread to run asynchronously; ugly and flaky;
    // is there a better way?
    Thread.sleep(1000);

    Assert.assertEquals(1, snapshots.myTable.getSelectedRow());
  }

  private static class MockDisposable implements Disposable {
    @Override
    public void dispose() {
    }
  }

  private static class MockActionManager extends ActionManagerEx {
    @Override
    public ActionPopupMenu createActionPopupMenu(String s, ActionGroup actionGroup) {
      return null;
    }

    @Override
    public ActionToolbar createActionToolbar(String s, ActionGroup actionGroup, boolean b) {
      return new MockActionToolbar();
    }

    @Override
    public AnAction getAction(String s) {
      return null;
    }

    @Override
    public String getId(AnAction anAction) {
      return null;
    }

    @Override
    public void registerAction(String s, AnAction anAction) {

    }

    @Override
    public void registerAction(String s, AnAction anAction, PluginId pluginId) {

    }

    @Override
    public void unregisterAction(String s) {

    }

    @Override
    public String[] getActionIds(String s) {
      return new String[0];
    }

    @Override
    public boolean isGroup(String s) {
      return false;
    }

    @Override
    public JComponent createButtonToolbar(String s, ActionGroup actionGroup) {
      return null;
    }

    @Override
    public AnAction getActionOrStub(String s) {
      return null;
    }

    @Override
    public void addTimerListener(int i, TimerListener timerListener) {

    }

    @Override
    public void removeTimerListener(TimerListener timerListener) {

    }

    @Override
    public void addTransparentTimerListener(int i, TimerListener timerListener) {

    }

    @Override
    public void removeTransparentTimerListener(TimerListener timerListener) {

    }

    @Override
    public ActionCallback tryToExecute(AnAction anAction, InputEvent inputEvent, Component component, String s, boolean b) {
      return null;
    }

    @Override
    public void addAnActionListener(AnActionListener anActionListener) {

    }

    @Override
    public void addAnActionListener(AnActionListener anActionListener, Disposable disposable) {

    }

    @Override
    public void removeAnActionListener(AnActionListener anActionListener) {

    }

    @Nullable
    @Override
    public KeyboardShortcut getKeyboardShortcut(String s) {
      return null;
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
      return null;
    }

    @Override
    public ActionToolbar createActionToolbar(String s, @NotNull ActionGroup actionGroup, boolean b, boolean b1) {
      return new MockActionToolbar();
    }

    @Override
    public void fireBeforeActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {

    }

    @Override
    public void fireAfterActionPerformed(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {

    }

    @Override
    public void fireBeforeEditorTyping(char c, DataContext dataContext) {

    }

    @Override
    public String getLastPreformedActionId() {
      return null;
    }

    @Override
    public String getPrevPreformedActionId() {
      return null;
    }

    @Override
    public Comparator<String> getRegistrationOrderComparator() {
      return null;
    }

    @NotNull
    @Override
    public String[] getPluginActions(PluginId pluginId) {
      return new String[0];
    }

    @Override
    public void queueActionPerformedEvent(AnAction anAction, DataContext dataContext, AnActionEvent anActionEvent) {

    }

    @Override
    public boolean isActionPopupStackEmpty() {
      return false;
    }

    @Override
    public boolean isTransparentOnlyActionsUpdateNow() {
      return false;
    }

    private static class MockActionToolbar implements ActionToolbar {
      @Override
      public JComponent getComponent() {
        return new JComponent() {
        };
      }

      @Override
      public boolean isCycleRoot() {
        return false;
      }

      @Override
      public int getLayoutPolicy() {
        return 0;
      }

      @Override
      public void setLayoutPolicy(int i) {

      }

      @Override
      public void adjustTheSameSize(boolean b) {

      }

      @Override
      public void setMinimumButtonSize(@NotNull Dimension dimension) {

      }

      @Override
      public void setOrientation(int i) {

      }

      @Override
      public int getMaxButtonHeight() {
        return 0;
      }

      @Override
      public void updateActionsImmediately() {

      }

      @Override
      public boolean hasVisibleActions() {
        return false;
      }

      @Override
      public void setTargetComponent(JComponent jComponent) {

      }

      @Override
      public void setReservePlaceAutoPopupIcon(boolean b) {

      }

      @Override
      public void setSecondaryActionsTooltip(String s) {

      }

      @Override
      public void setMiniMode(boolean b) {

      }

      @Override
      public DataContext getToolbarDataContext() {
        return null;
      }

      @Override
      public String getName() {
        return null;
      }

      @Override
      public List<AnAction> getActions(boolean b) {
        return null;
      }

      @Override
      public List<SwitchTarget> getTargets(boolean b, boolean b1) {
        return null;
      }

      @Override
      public SwitchTarget getCurrentTarget() {
        return null;
      }
    }
  }

  private class MockSession implements XDebugSession {
    @NotNull
    @Override
    public Project getProject() {
      DefaultPicoContainer container = new DefaultPicoContainer();
      container.registerComponentImplementation("com.intellij.openapi.roots.ProjectFileIndex", MockProjectFileIndex.class);
      return new MockProject(container, parent);
    }

    @NotNull
    @Override
    public XDebugProcess getDebugProcess() {
      return null;
    }

    @Override
    public boolean isSuspended() {
      return false;
    }

    @Nullable
    @Override
    public XStackFrame getCurrentStackFrame() {
      return null;
    }

    @Override
    public XSuspendContext getSuspendContext() {
      return null;
    }

    @Nullable
    @Override
    public XSourcePosition getCurrentPosition() {
      return null;
    }

    @Nullable
    @Override
    public XSourcePosition getTopFramePosition() {
      return null;
    }

    @Override
    public void stepOver(boolean b) {

    }

    @Override
    public void stepInto() {

    }

    @Override
    public void stepOut() {

    }

    @Override
    public void forceStepInto() {

    }

    @Override
    public void runToPosition(@NotNull XSourcePosition xSourcePosition, boolean b) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void showExecutionPoint() {

    }

    @Override
    public void setCurrentStackFrame(@NotNull XExecutionStack xExecutionStack, @NotNull XStackFrame xStackFrame, boolean b) {

    }

    @Override
    public void setCurrentStackFrame(@NotNull XExecutionStack xExecutionStack, @NotNull XStackFrame xStackFrame) {

    }

    @Override
    public void updateBreakpointPresentation(@NotNull XLineBreakpoint<?> xLineBreakpoint, @Nullable Icon icon, @Nullable String s) {

    }

    @Override
    public boolean breakpointReached(@NotNull XBreakpoint<?> xBreakpoint, @Nullable String s, @NotNull XSuspendContext xSuspendContext) {
      return false;
    }

    @Override
    public boolean breakpointReached(@NotNull XBreakpoint<?> xBreakpoint, @NotNull XSuspendContext xSuspendContext) {
      return false;
    }

    @Override
    public void positionReached(@NotNull XSuspendContext xSuspendContext) {

    }

    @Override
    public void sessionResumed() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setBreakpointMuted(boolean b) {

    }

    @Override
    public boolean areBreakpointsMuted() {
      return false;
    }

    @Override
    public void addSessionListener(@NotNull XDebugSessionListener xDebugSessionListener, @NotNull Disposable disposable) {

    }

    @Override
    public void addSessionListener(@NotNull XDebugSessionListener xDebugSessionListener) {

    }

    @Override
    public void removeSessionListener(@NotNull XDebugSessionListener xDebugSessionListener) {

    }

    @Override
    public void reportError(@NotNull String s) {

    }

    @Override
    public void reportMessage(@NotNull String s, @NotNull MessageType messageType) {

    }

    @Override
    public void reportMessage(@NotNull String s, @NotNull MessageType messageType, @Nullable HyperlinkListener hyperlinkListener) {

    }

    @NotNull
    @Override
    public String getSessionName() {
      return null;
    }

    @NotNull
    @Override
    public RunContentDescriptor getRunContentDescriptor() {
      return null;
    }

    @Nullable
    @Override
    public RunProfile getRunProfile() {
      return null;
    }

    @Override
    public void setPauseActionSupported(boolean b) {

    }

    @Override
    public void rebuildViews() {

    }

    @Override
    public <V extends XSmartStepIntoVariant> void smartStepInto(XSmartStepIntoHandler<V> xSmartStepIntoHandler, V v) {

    }

    @Override
    public void updateExecutionPosition() {

    }

    @Override
    public void initBreakpoints() {

    }

    @Override
    public ConsoleView getConsoleView() {
      return null;
    }

    @Override
    public RunnerLayoutUi getUI() {
      return null;
    }

    @Override
    public boolean isStopped() {
      return false;
    }

    @Override
    public boolean isPaused() {
      return false;
    }
  }

  private static class MockProjectFileIndex implements ProjectFileIndex {
    @Nullable
    @Override
    public Module getModuleForFile(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Nullable
    @Override
    public Module getModuleForFile(@NotNull VirtualFile virtualFile, boolean b) {
      return null;
    }

    @NotNull
    @Override
    public List<OrderEntry> getOrderEntriesForFile(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getClassRootForFile(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getSourceRootForFile(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getContentRootForFile(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getContentRootForFile(@NotNull VirtualFile virtualFile, boolean b) {
      return null;
    }

    @Nullable
    @Override
    public String getPackageNameByDirectory(@NotNull VirtualFile virtualFile) {
      return null;
    }

    @Override
    public boolean isLibraryClassFile(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isInSource(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isInLibraryClasses(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isInLibrarySource(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isIgnored(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isExcluded(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isUnderIgnored(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean iterateContent(@NotNull ContentIterator contentIterator) {
      return false;
    }

    @Override
    public boolean iterateContentUnderDirectory(@NotNull VirtualFile virtualFile, @NotNull ContentIterator contentIterator) {
      return false;
    }

    @Override
    public boolean isInContent(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isContentSourceFile(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isInSourceContent(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isInTestSourceContent(@NotNull VirtualFile virtualFile) {
      return false;
    }

    @Override
    public boolean isUnderSourceRootOfType(@NotNull VirtualFile virtualFile, @NotNull Set<? extends JpsModuleSourceRootType<?>> set) {
      return false;
    }
  }

  private static class MyMockApplicationEx extends MockApplicationEx {
    public MyMockApplicationEx(Disposable parent) {
      super(parent);
    }


    @Override
    public boolean runProcessWithProgressSynchronously(@NotNull Runnable task, @NotNull String progressTitle,
        boolean canBeCanceled, @Nullable Project project, JComponent parentComponent, String cancelText) {
      task.run();
      return true;
    }
  }
}
