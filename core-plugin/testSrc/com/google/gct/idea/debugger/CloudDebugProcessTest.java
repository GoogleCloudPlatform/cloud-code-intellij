package com.google.gct.idea.debugger;

import com.google.api.client.util.Lists;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

public class CloudDebugProcessTest extends PlatformTestCase {

    @Test
    public void testRegisterAdditionalActions_close() {
        assertRemoveFromLeftToolbar(IdeActions.ACTION_CLOSE);
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

    private void assertRemoveFromLeftToolbar(String actionId) {
        ActionManager manager = ActionManager.getInstance();
        XDebugSession session = Mockito.mock(XDebugSession.class);
        CloudDebugProcess process = new CloudDebugProcess(session);
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
          "Stop IDE Debugging. The server will continue to watch for breakpoints. " +
                  "(Exit debug mode and continue the debug session later.)",
          actual.toString());
    }

    private void assertRemoveFromTopToolbar(String actionId) {
        ActionManager manager = ActionManager.getInstance();
        XDebugSession session = Mockito.mock(XDebugSession.class);
        CloudDebugProcess process = new CloudDebugProcess(session);
        AnAction action = manager.getAction(actionId);
        List<AnAction> topToolbarActions = Lists.newArrayList();
        topToolbarActions.add(action);
        DefaultActionGroup topToolbar = new DefaultActionGroup(topToolbarActions);
        List actions = Lists.newArrayList();
        DefaultActionGroup leftToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        assertEmpty(topToolbar.getChildActionsOrStubs());
    }

}
