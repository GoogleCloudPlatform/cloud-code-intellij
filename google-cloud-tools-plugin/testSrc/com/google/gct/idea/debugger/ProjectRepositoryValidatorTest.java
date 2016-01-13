package com.google.gct.idea.debugger;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryValidatorTest {

  @Mock CloudDebugProcessState state;

  @Test
  public void testCheckSyncStashState_nullProject() {
    ProjectRepositoryValidator validator = new ProjectRepositoryValidator(state);
    SyncResult result = validator.checkSyncStashState();
    Assert.assertFalse(result.isValidDebuggee());
    Assert.assertFalse(result.needsStash());
    Assert.assertFalse(result.needsSync());
    Assert.assertFalse(result.hasLocalRepository());
    Assert.assertFalse(result.hasRemoteRepository());
    Assert.assertNull(result.getTargetSyncSHA());
    Assert.assertNull(result.getRepositoryType());
  }
}
