/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.login;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.debugger.CloudDebugProcessState;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessStateCollector;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IntelliJGoogleLoginMessageExtenderTest extends BasePluginTestCase {

  @Mock private CloudDebugProcessStateCollector stateCollector;

  @Before
  public void setUp() throws Exception {
    registerService(CloudDebugProcessStateCollector.class, stateCollector);
  }

  @Test
  public void testAdditionalLogoutMessage_returnsEmptyWhenNoStatesListening() throws Exception {
    assertThat(new IntelliJGoogleLoginMessageExtender().additionalLogoutMessage()).isEmpty();
  }

  @Test
  public void testAdditionalLogoutMessage_returnsMessageWhenAStateListens() throws Exception {
    when(stateCollector.getBackgroundListeningStates())
        .thenReturn(Collections.singletonList(mock(CloudDebugProcessState.class)));
    assertThat(new IntelliJGoogleLoginMessageExtender().additionalLogoutMessage())
        .isEqualTo("Any Stackdriver Debug sessions listening in the background will be stopped.");
  }
}
