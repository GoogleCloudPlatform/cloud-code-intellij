package com.google.gct.idea.debugger.ui;

import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.SourceLocation;
import com.google.gct.idea.debugger.CloudBreakpointHandler;
import com.google.gct.idea.debugger.CloudDebugProcess;
import com.google.gct.idea.debugger.CloudDebugProcessHandler;

import com.intellij.mock.MockApplication;
import com.intellij.mock.MockApplicationEx;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.xdebugger.XDebugSession;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

public class CloudDebugHistoricalSnapshotsTest {

  private Disposable parent = new MockDisposable();
  private CloudDebugProcess mockProcess = Mockito.mock(CloudDebugProcess.class);
  private CloudDebugProcessHandler handler = Mockito.mock(CloudDebugProcessHandler.class);

  @Before
  public void setUp() {
    MockApplication application = new MyMockApplicationEx(parent);
    ActionManagerEx manager = Mockito.mock(ActionManagerEx.class);
    ActionToolbar actionToolbar = Mockito.mock(ActionToolbar.class);
    Mockito.when(actionToolbar.getComponent()).thenReturn(new JComponent() {});
    Mockito.when(manager.createActionToolbar(Mockito.anyString(), Mockito.any(ActionGroup.class), Mockito.anyBoolean())).thenReturn(actionToolbar);
    Mockito.when(manager.createActionToolbar(Mockito.anyString(), Mockito.any(ActionGroup.class), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(actionToolbar);
    application.addComponent(ActionManager.class, manager);
    ApplicationManager.setApplication(application, parent);

    XDebugSession session = Mockito.mock(XDebugSession.class);
    Mockito.when(handler.getProcess()).thenReturn(mockProcess);
    Mockito.when(mockProcess.getXDebugSession()).thenReturn(session);
  }

  @After
  public void tearDown() {
    Disposer.dispose(parent);
  }

  @Test
  public void testOnBreakpointListChanged_noChanges() {
    CloudDebugHistoricalSnapshots snapshots = new CloudDebugHistoricalSnapshots(handler);
    runModelSetter(snapshots);

    Assert.assertNull(snapshots.myBalloon);
  }

  @Test
  public void testOnBreakpointListChanged() throws InterruptedException {
    CloudDebugHistoricalSnapshots snapshots = new CloudDebugHistoricalSnapshots(handler);

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

    runModelSetter(snapshots);

    Assert.assertEquals(0, snapshots.myTable.getSelectedRow());
  }

  @Test
  public void testOnBreakpointListChanged_twoBreakPoints() throws InterruptedException {
    CloudDebugHistoricalSnapshots snapshots = new CloudDebugHistoricalSnapshots(handler);

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

    CloudBreakpointHandler breakpointHandler = Mockito.mock(CloudBreakpointHandler.class);
    Mockito.when(mockProcess.getBreakpointHandler()).thenReturn(breakpointHandler);

    Assert.assertEquals(-1, snapshots.myTable.getSelectedRow());

    // BP1
    Mockito.when(mockProcess.getCurrentBreakpointList()).thenReturn(breakpoints1);
    Mockito.when(mockProcess.getCurrentSnapshot()).thenReturn(bp1);
    runModelSetter(snapshots);
    Assert.assertEquals(0, snapshots.myTable.getSelectedRow());

    // BP2
    Mockito.when(mockProcess.getCurrentBreakpointList()).thenReturn(breakpoints2);
    Mockito.when(mockProcess.getCurrentSnapshot()).thenReturn(bp2);
    runModelSetter(snapshots);
    Assert.assertEquals(1, snapshots.myTable.getSelectedRow());
  }

  private void runModelSetter(CloudDebugHistoricalSnapshots snapshots) {
    (snapshots.new ModelSetter(mockProcess.getCurrentBreakpointList(),
        snapshots.getSelection())).run();
  }

  private static class MockDisposable implements Disposable {
    @Override
    public void dispose() {
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
