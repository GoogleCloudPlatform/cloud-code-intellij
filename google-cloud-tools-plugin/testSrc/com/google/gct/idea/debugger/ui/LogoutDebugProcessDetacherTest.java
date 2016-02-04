package com.google.gct.idea.debugger.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.gct.idea.debugger.CloudDebugProcessHandler;
import com.google.gct.idea.testing.BasePluginTestCase;

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