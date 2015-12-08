package com.google.gct.idea.debugger;

import com.google.api.client.util.Lists;
import com.intellij.debugger.actions.DebuggerActions;
import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutStateDefaults;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

public class CloudDebugProcessTest extends PlatformTestCase {

    private CloudDebugProcess process;

    @Before
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        XDebugSession session = Mockito.mock(XDebugSession.class);
        Mockito.when(session.getProject()).thenReturn(this.getProject());
        process = new CloudDebugProcess(session);
        CloudDebugProcessState state = new CloudDebugProcessState(null, null, "project name", null, null);
        process.setProcessState(state);
    }

    @Test
    public void testRemoveConsolePane() {
        XDebugTabLayouter layouter = process.createTabLayouter();
        RunnerLayoutUi ui = Mockito.mock(RunnerLayoutUi.class);

        Content console = Mockito.mock(Content.class);
        Mockito.when(ui.findContent(DebuggerContentInfo.CONSOLE_CONTENT)).thenReturn(console);
        ui.removeContent(console, false);
        LayoutStateDefaults defaults = Mockito.mock(LayoutStateDefaults.class);
        Mockito.when(ui.getDefaults()).thenReturn(defaults);

        CloudDebugProcessState state = new CloudDebugProcessState(); //Mockito.mock(CloudDebugProcessState.class);
        process.initialize(state);

        layouter.registerAdditionalContent(ui);

        Mockito.verify(ui, Mockito.atLeast(1)).removeContent(console, false);
    }

    @Test
    public void testRegisterAdditionalActions_close() {
        ActionManager manager = ActionManager.getInstance();
        AnAction action0 = manager.getAction(IdeActions.ACTION_PIN_ACTIVE_TAB);
        AnAction action1 = manager.getAction(IdeActions.ACTION_CLOSE);
        action1.getTemplatePresentation().setText("Close");
        AnAction action2 = manager.getAction(IdeActions.ACTION_CONTEXT_HELP);
        List<AnAction> leftToolbarActions = Lists.newArrayList();
        leftToolbarActions.add(action0);
        leftToolbarActions.add(action1);
        leftToolbarActions.add(action2);
        DefaultActionGroup leftToolbar = new DefaultActionGroup(leftToolbarActions);
        List actions = Lists.newArrayList();
        DefaultActionGroup topToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        assertEquals(3, leftToolbar.getChildrenCount());
        assertEquals(action0, leftToolbar.getChildActionsOrStubs()[0]);
        assertEquals(action2, leftToolbar.getChildActionsOrStubs()[1]);
    }

    @Test
    public void testRegisterAdditionalActions_rerun() {
        assertRemoveFromLeftToolbar(IdeActions.ACTION_RERUN);
    }

    @Test
    public void testRegisterAdditionalActions_stop() {
        assertRemoveFromLeftToolbar(IdeActions.ACTION_STOP_PROGRAM);
    }

    @Test
    public void testRegisterAdditionalActions_resume() {
        assertRemoveFromLeftToolbar(XDebuggerActions.RESUME);
    }

    @Test
    public void testRegisterAdditionalActions_pause() {
        assertRemoveFromLeftToolbar(XDebuggerActions.PAUSE);
    }

    @Test
    public void testRegisterAdditionalActions_mute() {
        assertRemoveFromLeftToolbar(XDebuggerActions.MUTE_BREAKPOINTS);
    }

    @Test
    public void testRegisterAdditionalActions_forceStepInto() {
        assertRemoveFromTopToolbar(XDebuggerActions.FORCE_STEP_INTO);
    }

    @Test
    public void testRegisterAdditionalActions_stepOver() {
        assertRemoveFromTopToolbar(XDebuggerActions.STEP_OVER);
    }

    @Test
    public void testRegisterAdditionalActions_stepInto() {
        assertRemoveFromTopToolbar(XDebuggerActions.STEP_INTO);
    }

    @Test
    public void testRegisterAdditionalActions_stepOut() {
        assertRemoveFromTopToolbar(XDebuggerActions.STEP_OUT);
    }

    @Test
    public void testRegisterAdditionalActions_runToCursor() {
        assertRemoveFromTopToolbar(XDebuggerActions.RUN_TO_CURSOR);
    }

    @Test
    public void testRegisterAdditionalActions_evaluate() {
        assertRemoveFromTopToolbar(XDebuggerActions.EVALUATE_EXPRESSION);
    }

    @Test
    public void testRegisterAdditionalActions_dropFrame() {
        // name of constant "POP_FRAME" and UI label "Drop Frame" are inconsistent
        assertRemoveFromTopToolbar(DebuggerActions.POP_FRAME);
    }

    private void assertRemoveFromLeftToolbar(String actionId) {
        ActionManager manager = ActionManager.getInstance();
        AnAction action = manager.getAction(actionId);
        List<AnAction> leftToolbarActions = Lists.newArrayList();
        leftToolbarActions.add(action);
        DefaultActionGroup leftToolbar = new DefaultActionGroup(leftToolbarActions);
        List actions = Lists.newArrayList();
        DefaultActionGroup topToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        assertEquals(1, leftToolbar.getChildrenCount());
        AnAction actual = leftToolbar.getChildActionsOrStubs()[0];
        assertEquals(
          "Stop IDE Debugging. (Exit debug mode and continue the debug session later.)",
          actual.toString());
    }

    private void assertRemoveFromTopToolbar(String actionId) {
        ActionManager manager = ActionManager.getInstance();
        AnAction action = manager.getAction(actionId);
        List<AnAction> topToolbarActions = Lists.newArrayList();
        topToolbarActions.add(action);
        DefaultActionGroup topToolbar = new DefaultActionGroup(topToolbarActions);
        List actions = Lists.newArrayList();
        DefaultActionGroup leftToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/234
        AnAction[] childActionsOrStubs = topToolbar.getChildActionsOrStubs();
        assertEquals(1, childActionsOrStubs.length);
        assertTrue(childActionsOrStubs[0] instanceof LabelAction);
        assertTrue(childActionsOrStubs[0].displayTextInToolbar());
    }

}
