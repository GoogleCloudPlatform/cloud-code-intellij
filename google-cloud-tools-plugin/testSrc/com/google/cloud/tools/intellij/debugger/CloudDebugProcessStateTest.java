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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.FormatMessage;
import com.google.api.services.clouddebugger.v2.model.ListBreakpointsResponse;
import com.google.api.services.clouddebugger.v2.model.SourceLocation;
import com.google.api.services.clouddebugger.v2.model.StatusMessage;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.testing.TestUtils;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.GoogleLoginService;
import com.google.gdt.eclipse.login.common.GoogleLoginState;

import com.intellij.mock.MockProjectEx;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.util.xmlb.XmlSerializer;

import org.jdom.Element;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

/** Tests for {@link CloudDebugProcessState} */
public class CloudDebugProcessStateTest extends UsefulTestCase {
  private static final String USER = "user@gmail.com";
  private static final String PASSWORD = "123";
  private static final String DEBUGEE_ID = "debuggee_Id";
  private static final String PROJECT_NAME = "cloud-debugger-sample-project";
  private static final String PROJECT_NUMBER = "146354237";

  private MockProjectEx project;
  private IdeaProjectTestFixture fixture;

  @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
  public CloudDebugProcessStateTest() {
    IdeaTestCase.initPlatformPrefix();
  }

  private static boolean verifyList(List<Breakpoint> breakpoints, String... ids) {
    int bindex = 0;
    for (String id : ids) {
      if (!id.equals(breakpoints.get(bindex).getId())) {
        return false;
      }
      bindex++;
    }
    return bindex == breakpoints.size();
  }

  private static Breakpoint createBreakpoint(
      String id,
      Boolean isFinal,
      int finalTimeSeconds,
      String locationPath,
      Integer locationLine,
      Boolean isError,
      String statusMessage) {
    Breakpoint result = new Breakpoint();
    result.setId(id);
    result.setIsFinalState(isFinal);
    if (Boolean.TRUE.equals(isFinal)) {
      Calendar calendar =
          Calendar.getInstance(); // gets a calendar using the default time zone and locale.
      calendar.add(Calendar.SECOND, finalTimeSeconds);
      DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
      result.setFinalTime(formatter.print(calendar.getTimeInMillis()));
    }
    SourceLocation location = new SourceLocation();
    location.setPath(locationPath);
    location.setLine(locationLine);
    result.setLocation(location);
    StatusMessage status = new StatusMessage();
    status.setIsError(isError);
    FormatMessage message = new FormatMessage();
    message.setFormat(statusMessage);
    status.setDescription(message);
    result.setStatus(status);
    return result;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fixture =
        IdeaTestFixtureFactory.getFixtureFactory()
            .createFixtureBuilder(getTestName(true))
            .getFixture();
    fixture.setUp();

    project = new MockProjectEx(getTestRootDisposable());

    PsiManager psiManager = Mockito.mock(PsiManager.class);
    project.registerService(PsiManager.class, psiManager);

    GoogleLoginService mockLoginService = TestUtils.installMockService(GoogleLoginService.class);

    GoogleLoginState googleLoginState = Mockito.mock(GoogleLoginState.class);
    CredentialedUser user = Mockito.mock(CredentialedUser.class);
    LinkedHashMap<String, CredentialedUser> allusers =
        new LinkedHashMap<String, CredentialedUser>();

    when(user.getEmail()).thenReturn(USER);
    when(user.getGoogleLoginState()).thenReturn(googleLoginState);
    when(googleLoginState.fetchAccessToken()).thenReturn(PASSWORD);
    when(mockLoginService.getAllUsers()).thenReturn(allusers);
    allusers.put(USER, user);
  }

  @Override
  protected void tearDown() throws Exception {
    fixture.tearDown();
    fixture = null;
    super.tearDown();
  }

  public void testFirstSync() throws IOException {
    List<Breakpoint> returnedBreakpoints = new ArrayList<Breakpoint>();
    returnedBreakpoints.add(
        createBreakpoint("bp1", Boolean.TRUE, 1000, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint("bp2", Boolean.TRUE, 2000, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint(
            "bp3", Boolean.TRUE, 2200, "com/google/ex1.java", 15, Boolean.TRUE, "foo!"));
    returnedBreakpoints.add(
        createBreakpoint("bp4", Boolean.FALSE, 0, "com/google/ex2.java", 12, null, null));
    returnedBreakpoints.add(
        createBreakpoint("bp5", Boolean.FALSE, 0, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint("bp6", null, 0, "com/google/ex1.java", 16, null, null));
    returnedBreakpoints.add(
        createBreakpoint("bp7", Boolean.FALSE, 0, "com/google/ex1.java", 17, null, null));
    returnedBreakpoints.add(
        createBreakpoint("bp8", Boolean.FALSE, 0, "com/google/ex3.java", 18, null, null));
    Debugger client = createMockClient(returnedBreakpoints);

    CloudDebugProcessState state =
        new CloudDebugProcessState(USER, DEBUGEE_ID, PROJECT_NAME, PROJECT_NUMBER, null);
    assertEquals(USER, state.getUserEmail());
    // Danger: static global state
    CloudDebuggerClient.setClient(state.getUserEmail() + 120000, client);

    CloudDebugProcessStateController controller = new CloudDebugProcessStateController();
    controller.initialize(state);

    verify(client.debuggees().breakpoints().list(DEBUGEE_ID), times(1))
        .setIncludeInactive(Boolean.TRUE);

    List<Breakpoint> currentList = state.getCurrentServerBreakpointList();
    assertNotEmpty(currentList);

    //verifies the sort order...
    assertTrue(verifyList(currentList, "bp5", "bp6", "bp7", "bp4", "bp8", "bp3", "bp2", "bp1"));
  }

  public void testReOrderNoChange() throws IOException {
    List<Breakpoint> returnedBreakpoints = new ArrayList<Breakpoint>();

    returnedBreakpoints.add(
        createBreakpoint("p1", Boolean.TRUE, 1000, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint("p2", Boolean.TRUE, 2000, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint(
            "p3", Boolean.TRUE, 2200, "com/google/ex1.java", 15, Boolean.TRUE, "foo!"));
    returnedBreakpoints.add(
        createBreakpoint("p4", Boolean.FALSE, 0, "com/google/ex2.java", 12, null, null));
    returnedBreakpoints.add(
        createBreakpoint("p5", Boolean.FALSE, 0, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(createBreakpoint("p6", null, 0, "com/google/ex1.java", 16, null, null));
    returnedBreakpoints.add(
        createBreakpoint("p7", Boolean.FALSE, 0, "com/google/ex1.java", 17, null, null));
    returnedBreakpoints.add(
        createBreakpoint("p8", Boolean.FALSE, 0, "com/google/ex3.java", 18, null, null));
    Debugger client = createMockClient(returnedBreakpoints);

    CloudDebugProcessState state =
        new CloudDebugProcessState(USER, DEBUGEE_ID, PROJECT_NAME, PROJECT_NUMBER, null);
    assertEquals(USER, state.getUserEmail());
    // danger: static global state shared between tests
    CloudDebuggerClient.setClient(state.getUserEmail() + 120000, client);

    CloudDebugProcessStateController controller = new CloudDebugProcessStateController();
    controller.initialize(state);

    List<Breakpoint> currentList = state.getCurrentServerBreakpointList();
    assertNotEmpty(currentList);
    assertTrue(verifyList(currentList, "p5", "p6", "p7", "p4", "p8", "p3", "p2", "p1"));

    returnedBreakpoints.clear();
    returnedBreakpoints.add(
        createBreakpoint("b8", Boolean.FALSE, 0, "com/google/ex3.java", 18, null, null));
    returnedBreakpoints.add(
        createBreakpoint("b7", Boolean.FALSE, 0, "com/google/ex1.java", 17, null, null));
    returnedBreakpoints.add(createBreakpoint("b6", null, 0, "com/google/ex1.java", 16, null, null));
    returnedBreakpoints.add(
        createBreakpoint("b5", Boolean.FALSE, 0, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint("b4", Boolean.FALSE, 0, "com/google/ex2.java", 12, null, null));
    returnedBreakpoints.add(
        createBreakpoint(
            "b3", Boolean.TRUE, 2200, "com/google/ex1.java", 15, Boolean.TRUE, "foo!"));
    returnedBreakpoints.add(
        createBreakpoint("b2", Boolean.TRUE, 2000, "com/google/ex1.java", 15, null, null));
    returnedBreakpoints.add(
        createBreakpoint("b1", Boolean.TRUE, 1000, "com/google/ex1.java", 15, null, null));

    controller.waitForChanges();
    List<Breakpoint> changedList = state.getCurrentServerBreakpointList();
    assertNotEmpty(changedList);
    assertTrue(verifyList(changedList, "b5", "b6", "b7", "b4", "b8", "b3", "b2", "b1"));
  }

  public void testSerialization() throws IOException {
    CloudDebugProcessState state =
        new CloudDebugProcessState("emailUser", "debuggeeId", "projectName", "projectNumber", null);

    Element element = XmlSerializer.serialize(state);
    state = XmlSerializer.deserialize(element, CloudDebugProcessState.class);

    assertNotNull(state);
    assertTrue("debuggeeId".equals(state.getDebuggeeId()));
    assertTrue("emailUser".equals(state.getUserEmail()));
    assertTrue("projectName".equals(state.getProjectName()));
    assertTrue("projectNumber".equals(state.getProjectNumber()));
  }

  private Debugger createMockClient(final List<Breakpoint> returnedBreakpoints) throws IOException {
    Debugger client = Mockito.mock(Debugger.class);
    Debugger.Debuggees debuggees = Mockito.mock(Debugger.Debuggees.class);
    Debugger.Debuggees.Breakpoints breakpoints = Mockito.mock(Debugger.Debuggees.Breakpoints.class);
    Debugger.Debuggees.Breakpoints.List list =
        Mockito.mock(Debugger.Debuggees.Breakpoints.List.class);

    when(client.debuggees()).thenReturn(debuggees);
    when(debuggees.breakpoints()).thenReturn(breakpoints);
    when(breakpoints.list(DEBUGEE_ID)).thenReturn(list);
    when(list.setIncludeInactive(Boolean.TRUE)).thenReturn(list);
    when(list.setActionValue("CAPTURE")).thenReturn(list);
    when(list.setStripResults(Boolean.TRUE)).thenReturn(list);
    when(list.setWaitToken(null)).thenReturn(list);
    when(list.setClientVersion(
            ServiceManager.getService(CloudToolsPluginInfoService.class)
                .getClientVersionForCloudDebugger()))
        .thenReturn(list);
    when(list.execute())
        .thenAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                ListBreakpointsResponse response = new ListBreakpointsResponse();
                List<Breakpoint> copy = new ArrayList<Breakpoint>(returnedBreakpoints);
                response.setBreakpoints(copy);
                return response;
              }
            });
    return client;
  }
}
