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

import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.SourceLocation;
import com.intellij.mock.MockProjectEx;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.mockito.Mockito.when;

/**
 * Tests for {@link com.google.gct.idea.debugger.CloudBreakpointHandler}
 */
public class CloudBreakpointHandlerTest extends UsefulTestCase {

  private final Ref<Breakpoint> myAddedBp = new Ref<Breakpoint>();
  private final Ref<String> myRemovedBp = new Ref<String>();
  private MockProjectEx myProject;
  private CloudDebugProcess myProcess;
  private ArrayList<Breakpoint> myExistingBreakpoints;
  private PsiManager myPsiManager;
  private String myDesiredresultId;
  private IdeaProjectTestFixture myFixture;
  private CloudBreakpointHandler myHandler;

  public void testFoo() {
  }

// todo(elharo): understand why this code throws a NullPointerException and fix
/*
  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public CloudBreakpointHandlerTest() {
    IdeaTestCase.initPlatformPrefix();
  }


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getTestName(true)).getFixture();
    myFixture.setUp();

    myProject = new MockProjectEx(getTestRootDisposable());

    myPsiManager = Mockito.mock(PsiManager.class);
    myProject.registerService(PsiManager.class, myPsiManager);

    XDebugSession session = Mockito.mock(XDebugSession.class);
    when(session.getProject()).thenReturn(myProject);

    myProcess = Mockito.mock(CloudDebugProcess.class);
    when(myProcess.getXDebugSession()).thenReturn(session);
    CloudDebugProcessState processState = Mockito.mock(CloudDebugProcessState.class);
    myExistingBreakpoints = new ArrayList<Breakpoint>();
    when(processState.getCurrentServerBreakpointList()).thenReturn(
      ContainerUtil.immutableList(myExistingBreakpoints));
    when(myProcess.getProcessState()).thenReturn(processState);

    CloudDebugProcessStateController stateController = Mockito.mock(CloudDebugProcessStateController.class);
    when(myProcess.getStateController()).thenReturn(stateController);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        myAddedBp.set((Breakpoint)invocation.getArguments()[0]);
        SetBreakpointHandler handler = (SetBreakpointHandler)invocation.getArguments()[1];
        handler.onSuccess(myDesiredresultId);
        return null;
      }
    }).when(stateController).setBreakpointAsync(any(Breakpoint.class), any(SetBreakpointHandler.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        myRemovedBp.set((String)invocation.getArguments()[0]);
        return null;
      }
    }).when(stateController).deleteBreakpointAsync(anyString());

  }

  @Override
  protected void tearDown() throws Exception {
    myFixture.tearDown();
    myFixture = null;
    super.tearDown();
  } */

  public void testSimpleBreakpointRegister() {
    // todo(elharo): until we figure out how to fix this
  /*
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123, "foo.java", "com.google", false, "b_id");

    assertNull(myRemovedBp.get());
    assertNotNull(myAddedBp.get());
    assertContainsElements(myAddedBp.get().getExpressions(), "foowatch1");
    assertTrue(myAddedBp.get().getLocation().getLine() == 124);
    assertTrue(myAddedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(myAddedBp.get().getCondition().equals("condition == true"));
    */
  }

  public void ignore_testRegisterGetAndDelete() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123, "foo.java", "com.google", false, "b_id");

    assertNull(myRemovedBp.get());
    assertNotNull(myAddedBp.get());
    assertContainsElements(myAddedBp.get().getExpressions(), "foowatch1");
    assertTrue(myAddedBp.get().getLocation().getLine() == 124);
    assertTrue(myAddedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(myAddedBp.get().getCondition().equals("condition == true"));

    myAddedBp.get().setId("b_id");

    XBreakpoint xideBreakpoint = myHandler.getXBreakpoint(myAddedBp.get());
    assertNotNull(xideBreakpoint);
    myHandler.deleteBreakpoint(myAddedBp.get());

    assertNotNull(myRemovedBp.get());
    assertTrue(myRemovedBp.get() == myAddedBp.get().getId());
  }

  public void ignore_testServerCreation() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123, "foo.java", "com.google", true, "b_id");

    assertNull(myAddedBp.get());
  }

  public void ignore_testConflictingRegister() {
    Breakpoint existingServerBp = new Breakpoint();
    SourceLocation location = new SourceLocation();
    location.setPath("com/google/foo.java");
    location.setLine(124);
    existingServerBp.setLocation(location);
    existingServerBp.setId("todelete");
    myExistingBreakpoints.add(existingServerBp);

    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123, "foo.java", "com.google", false, "b_id");

    myExistingBreakpoints.clear();

    assertNotNull(myAddedBp.get());
    assertContainsElements(myAddedBp.get().getExpressions(), "foowatch1");
    assertTrue(myAddedBp.get().getLocation().getLine() == 124);
    assertTrue(myAddedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(myAddedBp.get().getCondition().equals("condition == true"));
  }

  @SuppressWarnings("unchecked")
  private void registerMockBreakpoint(String[] watches,
                                      String condition,
                                      int sourceLine,
                                      String shortFileName,
                                      String packageName,
                                      boolean createdByServer,
                                      String desiredresultId) {
    myDesiredresultId = desiredresultId;
    myAddedBp.set(null);
    myRemovedBp.set(null);

    myHandler = new CloudBreakpointHandler(myProcess);

    CloudLineBreakpointProperties properties = new CloudLineBreakpointProperties();
    properties.setWatchExpressions(watches);
    properties.setCreatedByServer(createdByServer);

    XLineBreakpointImpl lineBreakpoint = Mockito.mock(XLineBreakpointImpl.class);
    XExpression expression = Mockito.mock(XExpression.class);
    when(expression.getExpression()).thenReturn(condition);
    when(lineBreakpoint.getConditionExpression()).thenReturn(expression);
    when(lineBreakpoint.getProperties()).thenReturn(properties);
    when(lineBreakpoint.isEnabled()).thenReturn(Boolean.TRUE);
    when(lineBreakpoint.getType()).thenReturn(CloudLineBreakpointType.getInstance());

    VirtualFile mockFile = Mockito.mock(VirtualFile.class);
    XSourcePosition sourcePosition = Mockito.mock(XSourcePosition.class);
    when(sourcePosition.getLine()).thenReturn(sourceLine);
    when(sourcePosition.getFile()).thenReturn(mockFile);
    when(mockFile.isValid()).thenReturn(Boolean.TRUE);

    when(lineBreakpoint.getSourcePosition()).thenReturn(sourcePosition);

    PsiJavaFile psiJavaFile = Mockito.mock(PsiJavaFile.class);
    when(psiJavaFile.getPackageName()).thenReturn(packageName);
    when(psiJavaFile.getName()).thenReturn(shortFileName);
    when(myPsiManager.findFile(mockFile)).thenReturn(psiJavaFile);
    myHandler.setPsiManager(myPsiManager);

    CloudLineBreakpointType.CloudLineBreakpoint javaBreakpoint =
      (CloudLineBreakpointType.CloudLineBreakpoint)CloudLineBreakpointType.getInstance().createJavaBreakpoint(myProject, lineBreakpoint);
    when(lineBreakpoint.getUserData(com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY)).thenReturn(javaBreakpoint);

    myHandler.registerBreakpoint(lineBreakpoint);
  }
}
