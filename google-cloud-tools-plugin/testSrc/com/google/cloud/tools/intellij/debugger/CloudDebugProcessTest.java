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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.google.api.client.util.Lists;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.StatusMessage;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.cloud.tools.intellij.testing.TestUtils;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.intellij.debugger.actions.DebuggerActions;
import com.intellij.debugger.ui.DebuggerContentInfo;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.LayoutStateDefaults;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.ui.content.Content;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.actions.XDebuggerActions;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import com.intellij.xdebugger.ui.XDebugTabLayouter;

public class CloudDebugProcessTest extends PlatformTestCase {

    private CloudDebugProcess process;

    @Before
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        XDebugSession session = mock(XDebugSession.class);
        when(session.getProject()).thenReturn(this.getProject());
        process = new CloudDebugProcess(session);
    }
    
    @Test
    public void testRemoveConsolePane() {
        // if this was a JUnit4 test case, we could set the LoggedErrorProcessor to a mock that does
        // not fail the test if an error is logged using @BeforeClass. Since this is a JUnit3 test
        // case, we need to elaborately initialize a user in GoogleLogin
        CloudDebugProcessState state = new CloudDebugProcessState(); //Mockito.mock(CloudDebugProcessState.class);
        state.setUserEmail("mockUser@foo.com");

        CredentialedUser credentialedUser = mock(CredentialedUser.class);
        when(credentialedUser.getGoogleLoginState()).thenReturn(mock(GoogleLoginState.class));

        LinkedHashMap<String, CredentialedUser> users = new LinkedHashMap<String, CredentialedUser>();
        users.put(state.getUserEmail(), credentialedUser);

        GoogleLoginService mockGoogleLoginService = TestUtils
            .installMockService(GoogleLoginService.class);
        when(mockGoogleLoginService.getAllUsers()).thenReturn(users);
        process.initialize(state);

        XDebugTabLayouter layouter = process.createTabLayouter();
        RunnerLayoutUi ui = mock(RunnerLayoutUi.class);

        Content console = mock(Content.class);
        when(ui.findContent(DebuggerContentInfo.CONSOLE_CONTENT)).thenReturn(console);
        ui.removeContent(console, false);
        LayoutStateDefaults defaults = mock(LayoutStateDefaults.class);
        when(ui.getDefaults()).thenReturn(defaults);

        layouter.registerAdditionalContent(ui);

        verify(ui, Mockito.atLeast(1)).removeContent(console, false);
        
        process.getStateController().stopBackgroundListening();
    }

    @Test
    public void testRegisterAdditionalActions_close() {
        ActionManager manager = ActionManager.getInstance();
        AnAction action0 = manager.getAction(IdeActions.ACTION_PIN_ACTIVE_TAB);
        AnAction action1 = manager.getAction(IdeActions.ACTION_CLOSE);
        action1.getTemplatePresentation().setText("Close");
        AnAction action2 = manager.getAction(IdeActions.ACTION_CONTEXT_HELP);
        AnAction action3 = manager.getAction(IdeActions.ACTION_CALL_HIERARCHY);
        List<AnAction> leftToolbarActions = Lists.newArrayList();
        leftToolbarActions.add(action0);
        leftToolbarActions.add(action1);
        leftToolbarActions.add(action2);
        leftToolbarActions.add(action3);
        DefaultActionGroup leftToolbar = new DefaultActionGroup(leftToolbarActions);
        List<AnAction> actions = Lists.newArrayList();
        DefaultActionGroup topToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        assertEquals(4, leftToolbar.getChildrenCount());
        assertEquals(action0, leftToolbar.getChildActionsOrStubs()[0]);
        assertEquals(action3, leftToolbar.getChildActionsOrStubs()[1]);
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
        List<AnAction> actions = Lists.newArrayList();
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
        List<AnAction> actions = Lists.newArrayList();
        DefaultActionGroup leftToolbar = new DefaultActionGroup(actions);
        DefaultActionGroup settings = new DefaultActionGroup(actions);

        process.registerAdditionalActions(leftToolbar, topToolbar, settings);

        assertEmpty(topToolbar.getChildActionsOrStubs());
    }

    @Test
    public void testUpdateBreakpointRepresentationUsesBreakpointErrorMsgAndIcon() throws Exception {
        XBreakpointManager breakpointManager = mock(XBreakpointManager.class);
        CloudDebugProcess cloudDebugProcess = mockCloudDebugProcess(breakpointManager,
            mock(XDebugSession.class));

        CloudLineBreakpoint breakpoint = mockCloudLineBreakpoint("mock error message",
            mock(XLineBreakpointImpl.class));
        XLineBreakpoint xBreakpoint = mock(XLineBreakpoint.class);
        when(breakpoint.getXBreakpoint()).thenReturn(xBreakpoint);
        Icon icon = mock(Icon.class);
        when(breakpoint.getSetIcon(anyBoolean())).thenReturn(icon);
        cloudDebugProcess.updateBreakpointPresentation(breakpoint);

        verify(breakpoint).getXBreakpoint();
        verify(breakpoint).getSetIcon(Matchers.anyBoolean());
        verify(breakpoint).getErrorMessage();
        verify(breakpointManager).updateBreakpointPresentation(xBreakpoint, icon, "mock error message");
    }

    @Test
    public void testUpdateBreakpointRepresentationUsesMutedIconIfBreakpointsAreMuted() throws Exception {
        verifyMutedIconSettingInUpdateBreakpointPresentation(Boolean.TRUE);
    }

    @Test
    public void testUpdateBreakpointRepresentationUsesNonMutedIconIfBreakpointsAreNotMuted() throws Exception {
        verifyMutedIconSettingInUpdateBreakpointPresentation(Boolean.FALSE);
    }

    private void verifyMutedIconSettingInUpdateBreakpointPresentation(Boolean muted) {
        XBreakpointManager breakpointManager = mock(XBreakpointManager.class);
        XDebugSession debugSession = mock(XDebugSession.class);
        when(debugSession.areBreakpointsMuted()).thenReturn(muted);
        CloudDebugProcess cloudDebugProcess = mockCloudDebugProcess(breakpointManager,
            debugSession);

        CloudLineBreakpoint breakpoint = mockCloudLineBreakpoint("mock error message",
            mock(XLineBreakpointImpl.class));
        cloudDebugProcess.updateBreakpointPresentation(breakpoint);

        verify(breakpoint).getSetIcon(muted);
    }

    @NotNull
    private CloudDebugProcess mockCloudDebugProcess(XBreakpointManager breakpointManager,
        XDebugSession debugSession) {
        Project project = mock(Project.class);
        when(debugSession.getProject()).thenReturn(project);
        XDebuggerManager debuggerManager = mock(XDebuggerManager.class);
        when(project.getComponent(XDebuggerManager.class)).thenReturn(debuggerManager);
        when(debuggerManager.getBreakpointManager()).thenReturn(breakpointManager);
        return new CloudDebugProcess(debugSession);
    }

    @Test
    public void testOnBreakpointListChangedSetsErrorMessageAndUpdatesBreakpointPresentation()
        throws Exception {
        // override the default XBreakpointManager implementation with mock to use Mockito.verify()
        XBreakpointManager breakpointManager = mock(XBreakpointManager.class);
        XDebuggerManager debuggerManager = mock(XDebuggerManager.class);
        when(debuggerManager.getBreakpointManager()).thenReturn(breakpointManager);
        ((ProjectImpl)getProject()).registerComponentInstance(XDebuggerManager.class,
            debuggerManager);

        ArrayList<Breakpoint> breakpoints = new ArrayList<Breakpoint>();
        Breakpoint breakpoint = new Breakpoint();
        breakpoint.setId("breakpointId")
            .setIsFinalState(Boolean.TRUE)
            .setStatus(new StatusMessage().setIsError(Boolean.TRUE));
        breakpoints.add(breakpoint);
        CloudDebugProcessState processState = mock(CloudDebugProcessState.class);
        when(processState.getCurrentServerBreakpointList())
            .thenReturn(ContainerUtil.immutableList(breakpoints));

        XLineBreakpointImpl xLineBreakpointImpl = mock(XLineBreakpointImpl.class);
        CloudLineBreakpoint cloudLineBreakpoint = mockCloudLineBreakpoint("mock error message",
            xLineBreakpointImpl);
        when(xLineBreakpointImpl.getUserData(com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY))
            .thenReturn(cloudLineBreakpoint);
        CloudBreakpointHandler breakpointHandler = mock(CloudBreakpointHandler.class);
        when(breakpointHandler.getEnabledXBreakpoint(breakpoint)).thenReturn(xLineBreakpointImpl);

        process.setBreakpointHandler(breakpointHandler);
        process.initialize(processState);

        process.onBreakpointListChanged(mock(CloudDebugProcessState.class));

        verify(cloudLineBreakpoint).setErrorMessage(eq("General error"));
        verify(cloudLineBreakpoint).getXBreakpoint();
        verify(cloudLineBreakpoint).getSetIcon(Matchers.anyBoolean());
        verify(cloudLineBreakpoint).getErrorMessage();
        verify(breakpointManager).updateBreakpointPresentation(same(xLineBreakpointImpl), any(Icon.class), eq("General error"));
        
        process.getStateController().stopBackgroundListening(); 
    }

    @NotNull
    // mock behavior except for get/setErrorMessage() to simplify code
    private CloudLineBreakpoint mockCloudLineBreakpoint(String errorMessage,
                                                        XLineBreakpointImpl xLineBreakpoint) {
        CloudLineBreakpoint breakpoint = mock(CloudLineBreakpoint.class);
        when(breakpoint.getSetIcon(Matchers.anyBoolean())).thenReturn(mock(Icon.class));
        when(breakpoint.getXBreakpoint()).thenReturn(xLineBreakpoint);

        doCallRealMethod().when(breakpoint).setErrorMessage(anyString());
        when(breakpoint.getErrorMessage()).thenCallRealMethod();
        breakpoint.setErrorMessage(errorMessage);

        return breakpoint;
    }
}
