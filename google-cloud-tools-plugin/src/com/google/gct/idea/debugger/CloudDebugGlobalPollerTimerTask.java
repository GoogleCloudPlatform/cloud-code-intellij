package com.google.gct.idea.debugger;

import java.util.TimerTask;

/**
 * Polls for changes form the Cloud Debugger backend.
 */
class CloudDebugGlobalPollerTimerTask extends TimerTask {

  private final CloudDebugProcessStateCollector stateCollector;
  private CloudDebugGlobalPoller cloudDebugGlobalPoller;

  public CloudDebugGlobalPollerTimerTask(CloudDebugGlobalPoller cloudDebugGlobalPoller) {
    this.cloudDebugGlobalPoller = cloudDebugGlobalPoller;
    stateCollector = CloudDebugProcessStateCollector.getInstance();
  }

  @Override
  public void run() {
    for (CloudDebugProcessState state : stateCollector.getBackgroundListeningStates()) {
      cloudDebugGlobalPoller.pollForChanges(state);
    }
  }
}
