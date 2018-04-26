/*
 * Copyright 2018 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.stackdriver.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CloudDebugGlobalPollerTimerTaskTest extends BasePluginTestCase {

  @Mock private CloudDebugGlobalPoller cloudDebugGlobalPoller;
  @Mock private CloudDebugProcessStateCollector cloudDebugProcessStateCollector;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    registerService(CloudDebugProcessStateCollector.class, cloudDebugProcessStateCollector);
  }

  @Test
  public void testRunCallsPollforChangesForAllStates() throws Exception {
    List<CloudDebugProcessState> states = new ArrayList<CloudDebugProcessState>();
    states.add(mock(CloudDebugProcessState.class));
    states.add(mock(CloudDebugProcessState.class));
    when(cloudDebugProcessStateCollector.getBackgroundListeningStates()).thenReturn(states);

    new CloudDebugGlobalPollerTimerTask(cloudDebugGlobalPoller).run();

    for (CloudDebugProcessState cloudDebugProcessState : states) {
      verify(cloudDebugGlobalPoller).pollForChanges(cloudDebugProcessState);
    }
  }
}
