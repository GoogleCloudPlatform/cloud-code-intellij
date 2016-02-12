package com.google.gct.idea.debugger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gct.idea.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class CloudDebugGlobalPollerTimerTaskTest extends BasePluginTestCase {

  @Mock
  private CloudDebugGlobalPoller cloudDebugGlobalPoller;
  @Mock
  private CloudDebugProcessStateCollector cloudDebugProcessStateCollector;

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
