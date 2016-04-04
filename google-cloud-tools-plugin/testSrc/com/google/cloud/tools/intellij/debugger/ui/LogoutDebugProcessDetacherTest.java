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

package com.google.cloud.tools.intellij.debugger.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.cloud.tools.intellij.debugger.CloudDebugProcessHandler;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LogoutDebugProcessDetacherTest extends BasePluginTestCase {

  @Mock
  private CloudDebugProcessHandler processHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testOnLogoutDebugProcessIsDetached() throws Exception {
    new LogoutDebugProcessDetacher<CloudDebugProcessHandler>(processHandler).statusChanged(false);
    verify(processHandler).detachProcess();
  }

  @Test
  public void testOnLoginDebugProcessIsNotChanged() throws Exception {
    new LogoutDebugProcessDetacher<CloudDebugProcessHandler>(processHandler).statusChanged(true);
    verifyNoMoreInteractions(processHandler);
  }
}