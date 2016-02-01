package com.google.gct.idea.debugger;

import java.util.TimerTask;

/**
 * Polls for changes form the Cloud Debugger backend.
 */
class CloudDebugGlobalPollerTimerTask extends TimerTask {

  private CloudDebugGlobalPoller cloudDebugGlobalPoller;

  public CloudDebugGlobalPollerTimerTask(CloudDebugGlobalPoller cloudDebugGlobalPoller) {
    this.cloudDebugGlobalPoller = cloudDebugGlobalPoller;
  }

  @Override
  public void run() {
    for (CloudDebugProcessState state : cloudDebugGlobalPoller.getStates()) {
      cloudDebugGlobalPoller.pollForChanges(state);
    }
  }
}
