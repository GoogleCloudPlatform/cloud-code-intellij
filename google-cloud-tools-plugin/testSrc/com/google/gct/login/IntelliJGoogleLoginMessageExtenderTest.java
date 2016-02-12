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

package com.google.gct.login;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gct.idea.debugger.CloudDebugProcessState;
import com.google.gct.idea.debugger.CloudDebugProcessStateCollector;
import com.google.gct.idea.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class IntelliJGoogleLoginMessageExtenderTest extends BasePluginTestCase {

  @Mock
  private CloudDebugProcessStateCollector stateCollector;

  @Before
  public void setUp() throws Exception {
    registerService(CloudDebugProcessStateCollector.class, stateCollector);

  }

  @Test
  public void testAdditionalLogoutMessage_returnsEmptyWhenNoStatesListening() throws Exception {
    assertThat(new IntelliJGoogleLoginMessageExtender().additionalLogoutMessage(), isEmptyString());
  }

  @Test
  public void testAdditionalLogoutMessage_returnsMessageWhenAStateListens() throws Exception {
    when(stateCollector.getBackgroundListeningStates())
        .thenReturn(Collections.singletonList(mock(CloudDebugProcessState.class)));
    assertThat(new IntelliJGoogleLoginMessageExtender().additionalLogoutMessage(),
               is("Any Cloud Debugger sessions listening in the background will be stopped."));
  }

}
