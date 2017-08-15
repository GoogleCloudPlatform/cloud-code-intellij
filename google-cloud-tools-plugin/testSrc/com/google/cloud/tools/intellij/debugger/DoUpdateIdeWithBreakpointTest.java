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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashMap;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

/** Unit tests for {@link DoUpdateIdeWithBreakpoint}. */
@RunWith(JUnit4.class)
public final class DoUpdateIdeWithBreakpointTest {

  private static final String MOCK_FILE_URL = "/tmp/mockFilePath";
  private static final String MOCK_BREAKPOINT_CONDITION = "mock breakpoint condition";
  private static final String MOCK_BREAKPOINT_EXPRESSION = "mock breakpoint expression";

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock private XLineBreakpoint<CloudLineBreakpointProperties> xLineBreakpoint;
  @Mock private CloudLineBreakpoint cloudLineBreakpoint;
  @Mock private XBreakpointManager breakpointManager;
  @Mock private CloudDebugProcess cloudDebugProcess;
  @Mock private VirtualFile virtualFile;

  @Captor private ArgumentCaptor<Key<String>> keyArgumentCaptor;

  private final CloudLineBreakpointProperties cloudLineBreakpointProperties =
      new CloudLineBreakpointProperties();

  @Test
  public void runUpdateIdeWithBreakpoint() throws Exception {
    when(virtualFile.getUrl()).thenReturn(MOCK_FILE_URL);

    when(xLineBreakpoint.getUserData(com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY))
        .thenReturn(cloudLineBreakpoint);
    when(xLineBreakpoint.getProperties()).thenReturn(cloudLineBreakpointProperties);

    when(breakpointManager.addLineBreakpoint(
            any(CloudLineBreakpointType.class),
            anyString(),
            anyInt(),
            eq(cloudLineBreakpointProperties)))
        .thenReturn(xLineBreakpoint);

    Breakpoint serverBreakpoint = new Breakpoint();
    serverBreakpoint
        .setId("mock-breakpoint-id")
        .setCondition(MOCK_BREAKPOINT_CONDITION)
        .setExpressions(Lists.newArrayList(MOCK_BREAKPOINT_EXPRESSION));

    HashMap<String, XBreakpoint> ideBreakpoints = new HashMap<>();

    int line = 1;

    new DoUpdateIdeWithBreakpoint(
            breakpointManager,
            virtualFile,
            line,
            cloudLineBreakpointProperties,
            serverBreakpoint,
            ideBreakpoints,
            cloudDebugProcess)
        .run();

    verify(breakpointManager)
        .addLineBreakpoint(any(), eq(MOCK_FILE_URL), eq(line), eq(cloudLineBreakpointProperties));

    verify(xLineBreakpoint).putUserData(keyArgumentCaptor.capture(), eq("mock-breakpoint-id"));
    assertThat(keyArgumentCaptor.getValue().toString())
        .isEqualTo("CloudId"); // CloudBreakpointHandler.CLOUD_ID

    assertThat(ideBreakpoints.get("mock-breakpoint-id")).isEqualTo(xLineBreakpoint);

    verify(xLineBreakpoint).setCondition(eq(MOCK_BREAKPOINT_CONDITION));

    assertThat(cloudLineBreakpointProperties.getWatchExpressions()).hasLength(1);
    assertThat(cloudLineBreakpointProperties.getWatchExpressions()[0])
        .isEqualTo(MOCK_BREAKPOINT_EXPRESSION);
    assertFalse(cloudLineBreakpointProperties.isCreatedByServer());

    verify(cloudDebugProcess).updateBreakpointPresentation(cloudLineBreakpoint);
  }
}
