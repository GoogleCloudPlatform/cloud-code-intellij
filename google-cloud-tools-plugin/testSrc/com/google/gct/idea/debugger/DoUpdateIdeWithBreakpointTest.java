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

package com.google.gct.idea.debugger;

import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.common.collect.Lists;
import com.google.gct.idea.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.gct.idea.testing.BasePluginTestCase;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashMap;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DoUpdateIdeWithBreakpointTest extends BasePluginTestCase {

  private static final String MOCK_FILE_URL = "/tmp/mockFilePath";
  private static final String MOCK_BREAKPOINT_CONDITION = "mock breakpoint condition";
  private static final String MOCK_BREAKPOINT_EXPRESSION = "mock breakpoint expression";

  @Mock
  private XLineBreakpoint<CloudLineBreakpointProperties> xLineBreakpoint;
  @Mock
  private CloudLineBreakpoint cloudLineBreakpoint;
  @Mock
  private XBreakpointManager breakpointManager;
  @Mock
  private CloudDebugProcess cloudDebugProcess;
  @Mock
  private VirtualFile virtualFile;

  @Captor
  private ArgumentCaptor<Key<String>> keyArgumentCaptor;

  private CloudLineBreakpointProperties cloudLineBreakpointProperties;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    cloudLineBreakpointProperties = new CloudLineBreakpointProperties();
  }

  @Test
  public void testRunUpdateIdeWithBreakpoint() throws Exception {
    registerExtensionPoint(XBreakpointType.EXTENSION_POINT_NAME, CloudLineBreakpointType.class);

    when(virtualFile.getUrl()).thenReturn(MOCK_FILE_URL);

    when(xLineBreakpoint.getUserData(com.intellij.debugger.ui.breakpoints.Breakpoint.DATA_KEY))
        .thenReturn(cloudLineBreakpoint);
    when(xLineBreakpoint.getProperties()).thenReturn(cloudLineBreakpointProperties);

    when(breakpointManager.addLineBreakpoint(any(CloudLineBreakpointType.class),
                                             anyString(),
                                             anyInt(),
                                             eq(cloudLineBreakpointProperties)))
        .thenReturn(xLineBreakpoint);

    Breakpoint serverBreakpoint = new Breakpoint();
    serverBreakpoint.setId("mock-breakpoint-id")
        .setCondition(MOCK_BREAKPOINT_CONDITION)
        .setExpressions(Lists.newArrayList(MOCK_BREAKPOINT_EXPRESSION));

    HashMap<String, XBreakpoint> ideBreakpoints = new HashMap<String, XBreakpoint>();

    int line = 1;

    new DoUpdateIdeWithBreakpoint(breakpointManager,
                                      virtualFile,
                                      line,
                                      cloudLineBreakpointProperties,
                                      serverBreakpoint,
                                      ideBreakpoints,
                                      cloudDebugProcess)
        .run();

    verify(breakpointManager).addLineBreakpoint(any(CloudLineBreakpointType.class),
                                                eq(MOCK_FILE_URL),
                                                eq(line),
                                                eq(cloudLineBreakpointProperties));

    verify(xLineBreakpoint).putUserData(keyArgumentCaptor.capture(), eq("mock-breakpoint-id"));
    assertThat(keyArgumentCaptor.getValue().toString(), equalTo("CloudId")); //CloudBreakpointHandler.CLOUD_ID

    assertThat(ideBreakpoints.get("mock-breakpoint-id"),
               IsEqual.<XBreakpoint>equalTo(xLineBreakpoint));

    verify(xLineBreakpoint).setCondition(eq(MOCK_BREAKPOINT_CONDITION));

    assertThat(cloudLineBreakpointProperties.getWatchExpressions(),
               arrayWithSize(1));
    assertThat(cloudLineBreakpointProperties.getWatchExpressions()[0],
               is(MOCK_BREAKPOINT_EXPRESSION));
    assertFalse(cloudLineBreakpointProperties.isCreatedByServer());

    verify(cloudDebugProcess).updateBreakpointPresentation(cloudLineBreakpoint);
  }


}