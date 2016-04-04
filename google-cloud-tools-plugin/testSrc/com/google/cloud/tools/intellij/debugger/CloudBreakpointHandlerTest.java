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

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.SourceLocation;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessStateController.SetBreakpointHandler;

import com.intellij.mock.MockProjectEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;

import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link CloudBreakpointHandler}
 */
public class CloudBreakpointHandlerTest extends UsefulTestCase {

  private final Ref<Breakpoint> addedBp = new Ref<Breakpoint>();
  private final Ref<String> removedBp = new Ref<String>();
  private final String NO_CONDITION = null;
  private final String[] NO_WATCHES = null;
  private MockProjectEx project;
  private CloudDebugProcess process;
  private ArrayList<Breakpoint> existingBreakpoints;
  private PsiManager psiManager;
  private String desiredResultId;
  private IdeaProjectTestFixture fixture;
  private CloudBreakpointHandler handler;
  private CloudDebugProcessStateController stateController;
  private boolean registrationShouldSucceed;

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public CloudBreakpointHandlerTest() {
    IdeaTestCase.initPlatformPrefix();
  }

  private XBreakpointManager breakpointManager;
  private ServerToIDEFileResolver fileResolver;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(
        getTestName(true)).getFixture();
    fixture.setUp();

    project = new MockProjectEx(getTestRootDisposable());

    psiManager = mock(PsiManager.class);
    project.registerService(PsiManager.class, psiManager);

    XDebugSession session = mock(XDebugSession.class);
    when(session.getProject()).thenReturn(project);

    process = mock(CloudDebugProcess.class);
    when(process.getXDebugSession()).thenReturn(session);
    CloudDebugProcessState processState = mock(CloudDebugProcessState.class);
    existingBreakpoints = new ArrayList<Breakpoint>();
    when(processState.getCurrentServerBreakpointList()).thenReturn(
      ContainerUtil.immutableList(existingBreakpoints));
    when(process.getProcessState()).thenReturn(processState);

    stateController = mock(CloudDebugProcessStateController.class);
    when(process.getStateController()).thenReturn(stateController);

    registrationShouldSucceed = true;

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        addedBp.set((Breakpoint)invocation.getArguments()[0]);
        SetBreakpointHandler handler = (SetBreakpointHandler)invocation.getArguments()[1];
        if (registrationShouldSucceed) {
          handler.onSuccess(desiredResultId);
        } else {
          handler.onError("Registration failed");
        }
        return null;
      }
    }).when(stateController).setBreakpointAsync(
        any(Breakpoint.class), any(SetBreakpointHandler.class));

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        removedBp.set((String)invocation.getArguments()[0]);
        return null;
      }
    }).when(stateController).deleteBreakpointAsync(anyString());

    fileResolver = mock(ServerToIDEFileResolver.class);
    handler = new CloudBreakpointHandler(process, fileResolver);

    XDebuggerManager debuggerManager = mock(XDebuggerManager.class);
    project.addComponent(XDebuggerManager.class, debuggerManager);
    breakpointManager = mock(XBreakpointManager.class);
    when(debuggerManager.getBreakpointManager()).thenReturn(breakpointManager);
  }

  @Override
  protected void tearDown() throws Exception {
    fixture.tearDown();
    fixture = null;
    super.tearDown();
  }

  public void testSimpleBreakpointRegister() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", false, "b_id");

    assertNull(removedBp.get());
    assertNotNull(addedBp.get());
    assertContainsElements(addedBp.get().getExpressions(), "foowatch1");
    assertTrue(addedBp.get().getLocation().getLine() == 124);
    assertTrue(addedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(addedBp.get().getCondition().equals("condition == true"));

    ArgumentCaptor<CloudLineBreakpoint> breakpointArgumentCaptor = ArgumentCaptor.forClass(CloudLineBreakpoint.class);
    verify(process).updateBreakpointPresentation(breakpointArgumentCaptor.capture());
    assertThat(breakpointArgumentCaptor.getValue().getErrorMessage(), nullValue());
  }

  public void testRegisterBreakpointErrorShouldSetErrorMessageAndCallUpdateBreakpointPresentation() {
    registrationShouldSucceed = false;
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", false, "b_id");

    ArgumentCaptor<CloudLineBreakpoint> breakpointArgumentCaptor = ArgumentCaptor.forClass(CloudLineBreakpoint.class);
    verify(process).updateBreakpointPresentation(breakpointArgumentCaptor.capture());
    assertThat(breakpointArgumentCaptor.getValue().getErrorMessage(), equalTo("Registration failed"));
  }

  public void testRegisterBreakpointErrorShouldSetErrorMessageAndCallUpdateBreakpointPresentation2() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", false, "");

    ArgumentCaptor<CloudLineBreakpoint> breakpointArgumentCaptor = ArgumentCaptor.forClass(CloudLineBreakpoint.class);
    verify(process).updateBreakpointPresentation(breakpointArgumentCaptor.capture());
    assertThat(breakpointArgumentCaptor.getValue().getErrorMessage(), equalTo("The snapshot location could not be set."));
  }

  public void testRegisterGetAndDelete() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", false, "b_id");

    assertNull(removedBp.get());
    assertNotNull(addedBp.get());
    assertContainsElements(addedBp.get().getExpressions(), "foowatch1");
    assertTrue(addedBp.get().getLocation().getLine() == 124);
    assertTrue(addedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(addedBp.get().getCondition().equals("condition == true"));

    addedBp.get().setId("b_id");

    XBreakpoint xideBreakpoint = handler.getXBreakpoint(addedBp.get());
    assertNotNull(xideBreakpoint);
    handler.deleteBreakpoint(addedBp.get());

    assertNotNull(removedBp.get());
    assertTrue(removedBp.get() == addedBp.get().getId());
  }

  public void testServerCreation() {
    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", true, "b_id");

    assertNull(addedBp.get());
  }

  public void testConflictingRegister() {
    Breakpoint existingServerBp = new Breakpoint();
    SourceLocation location = new SourceLocation();
    location.setPath("com/google/foo.java");
    location.setLine(124);
    existingServerBp.setLocation(location);
    existingServerBp.setId("todelete");
    existingBreakpoints.add(existingServerBp);

    registerMockBreakpoint(new String[]{"foowatch1"}, "condition == true", 123,
        "foo.java", "com.google", false, "b_id");

    existingBreakpoints.clear();

    assertNotNull(addedBp.get());
    assertContainsElements(addedBp.get().getExpressions(), "foowatch1");
    assertTrue(addedBp.get().getLocation().getLine() == 124);
    assertTrue(addedBp.get().getLocation().getPath().equals("com/google/foo.java"));
    assertTrue(addedBp.get().getCondition().equals("condition == true"));
  }

  // Tries to register an already registered breakpoint and only first registration goes through.
  // https://github.com/GoogleCloudPlatform/gcloud-intellij/issues/142
  public void testRegisterRegisteredBreakpoint() {
    XLineBreakpointImpl<CloudLineBreakpointProperties> breakpoint = registerMockBreakpoint(
        NO_WATCHES, NO_CONDITION, 13, "fileName", "packageName", false, "12abc");
    handler.registerBreakpoint(breakpoint);
    verify(stateController, times(1)).setBreakpointAsync(
        isA(Breakpoint.class), isA(SetBreakpointHandler.class));
  }

  public void testRegisterRegisteredButDisabledBreakpoint() {
    XLineBreakpointImpl<CloudLineBreakpointProperties> breakpoint = registerMockBreakpoint(
        NO_WATCHES, NO_CONDITION, 13, "fileName", "packageName", false, "12abc");
    handler.setStateToDisabled(new Breakpoint().setId("12abc"));
    handler.registerBreakpoint(breakpoint);
    verify(stateController, times(2)).setBreakpointAsync(
        isA(Breakpoint.class), isA(SetBreakpointHandler.class));
  }

  public void testCreateIdeRepresentationsIfNecessaryVerifiesNonFinalIdeBreakpoint()
      throws Exception {
    XLineBreakpointImpl breakpoint = registerMockBreakpoint(NO_WATCHES, NO_CONDITION, 13,
        "fileName", "packageName", false, "12abc");

    CloudLineBreakpoint cloudLineBreakpoint = (CloudLineBreakpoint) breakpoint
        .getUserData(com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY);
    assertNotNull(cloudLineBreakpoint);
    Assert.assertFalse(cloudLineBreakpoint.isVerified());

    handler.createIdeRepresentationsIfNecessary(Lists.newArrayList(new Breakpoint().setId("12abc")));

    assertThat(cloudLineBreakpoint.getErrorMessage(), nullValue());
    Assert.assertTrue(cloudLineBreakpoint.isVerified());
  }

  @SuppressWarnings("unchecked")
  public void testUnregisterBreakpoint_shouldSetAddedOnServerToFalseAfterHitOnBackend() throws Exception {
    XLineBreakpointImpl breakpoint = registerMockBreakpoint(NO_WATCHES, NO_CONDITION, 13,
        "fileName", "packageName", false, "12abc");
    handler.setStateToDisabled(new Breakpoint().setId("12abc"));
    assertNotNull(breakpoint.getProperties());
    assertTrue(((CloudLineBreakpointProperties) breakpoint.getProperties()).isAddedOnServer());

    handler.unregisterBreakpoint(breakpoint, false);

    assertFalse(((CloudLineBreakpointProperties) breakpoint.getProperties()).isAddedOnServer());
    verify(stateController, never()).deleteBreakpointAsync("12abc");
  }

  @SuppressWarnings("unchecked")
  public void testUnregisterBreakpoint_shouldSetAddedOnServerToFalseAfterClientDisablesBp() throws Exception {
    XLineBreakpointImpl breakpoint = registerMockBreakpoint(NO_WATCHES, NO_CONDITION, 13,
        "fileName", "packageName", false, "12abc");
    assertNotNull(breakpoint.getProperties());
    assertTrue(((CloudLineBreakpointProperties) breakpoint.getProperties()).isAddedOnServer());

    handler.unregisterBreakpoint(breakpoint, false);

    assertFalse(((CloudLineBreakpointProperties) breakpoint.getProperties()).isAddedOnServer());
    verify(stateController).deleteBreakpointAsync("12abc");
  }

  @SuppressWarnings("unchecked")
  private XLineBreakpointImpl<CloudLineBreakpointProperties> registerMockBreakpoint(String[] watches,
                                      String condition,
                                      int sourceLine,
                                      String shortFileName,
                                      String packageName,
                                      boolean createdByServer,
                                      String desiredResultId) {
    this.desiredResultId = desiredResultId;
    addedBp.set(null);
    removedBp.set(null);

    CloudLineBreakpointProperties properties = new CloudLineBreakpointProperties();
    properties.setWatchExpressions(watches);
    properties.setCreatedByServer(createdByServer);

    XLineBreakpointImpl<CloudLineBreakpointProperties> lineBreakpoint = mock(XLineBreakpointImpl.class);
    XExpression expression = mock(XExpression.class);
    when(expression.getExpression()).thenReturn(condition);
    when(lineBreakpoint.getConditionExpression()).thenReturn(expression);
    when(lineBreakpoint.getProperties()).thenReturn(properties);
    when(lineBreakpoint.isEnabled()).thenReturn(Boolean.TRUE);
    when(lineBreakpoint.getType()).thenReturn(CloudLineBreakpointType.getInstance());

    VirtualFile mockFile = mock(VirtualFile.class);
    XSourcePosition sourcePosition = mock(XSourcePosition.class);
    when(sourcePosition.getLine()).thenReturn(sourceLine);
    when(sourcePosition.getFile()).thenReturn(mockFile);
    when(mockFile.isValid()).thenReturn(Boolean.TRUE);

    when(lineBreakpoint.getSourcePosition()).thenReturn(sourcePosition);

    PsiJavaFile psiJavaFile = mock(PsiJavaFile.class);
    when(psiJavaFile.getPackageName()).thenReturn(packageName);
    when(psiJavaFile.getName()).thenReturn(shortFileName);
    when(psiManager.findFile(mockFile)).thenReturn(psiJavaFile);
    handler.setPsiManager(psiManager);

    CloudLineBreakpointType.CloudLineBreakpoint javaBreakpoint =
        (CloudLineBreakpointType.CloudLineBreakpoint) CloudLineBreakpointType
            .getInstance().createJavaBreakpoint(project, lineBreakpoint);
    when(lineBreakpoint.getUserData(
        com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY)).thenReturn(javaBreakpoint);
    when(lineBreakpoint.getUserData(CloudBreakpointHandler.CLOUD_ID)).thenReturn(desiredResultId);

    handler.registerBreakpoint(lineBreakpoint);
    return lineBreakpoint;
  }

  public void testCreateIdeRepresentationsIfNecessary() {
    List<Breakpoint> breakpoints = ImmutableList.of(
        new Breakpoint().setId("expected_path").setLocation(
            new SourceLocation().setLine(1).setPath("app/mod/src/main/java/b/f/pkg/Class.java")),
        new Breakpoint().setId("unexpected_path").setLocation(
            new SourceLocation().setLine(2).setPath("app/mod/src/m/j/a/b/c/Class.java")),
        new Breakpoint().setId("unexpected_path_2").setLocation(
            new SourceLocation().setLine(3).setPath("b/f/pkg/Class.java")
        ));
    when(breakpointManager.findBreakpointAtLine(
        isA(XLineBreakpointType.class), isA(VirtualFile.class), anyInt())).thenReturn(null);
    XLineBreakpoint mockLineBreakpoint = mock(XLineBreakpoint.class);
    when(breakpointManager.addLineBreakpoint(isA(XLineBreakpointType.class), anyString(), anyInt(),
        isA(CloudLineBreakpointProperties.class))).thenReturn(mockLineBreakpoint);
    when(mockLineBreakpoint.getProperties()).thenReturn(new CloudLineBreakpointProperties());
    VirtualFile projectDir = mock(VirtualFile.class);
    when(projectDir.getPath()).thenReturn("/project/dir");
    project.setBaseDir(projectDir);
    VirtualFile classFile = mock(VirtualFile.class);
    when(classFile.getUrl()).thenReturn("file:///URL");
    when(fileResolver.getFileFromPath(
        isA(Project.class), eq("app/mod/src/main/java/b/f/pkg/Class.java")))
        .thenReturn(classFile);

    handler.createIdeRepresentationsIfNecessary(breakpoints);

    verify(breakpointManager, times(1)).addLineBreakpoint(isA(XLineBreakpointType.class),
        anyString(), anyInt(), isA(XBreakpointProperties.class));
  }
}
