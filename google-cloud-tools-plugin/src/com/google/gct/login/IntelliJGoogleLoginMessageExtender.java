package com.google.gct.login;

import com.google.gct.idea.debugger.CloudDebugProcessStateCollector;
import com.google.gct.idea.util.GctBundle;

public class IntelliJGoogleLoginMessageExtender implements GoogleLoginMessageExtender {

  private final CloudDebugProcessStateCollector stateCollector;

  public IntelliJGoogleLoginMessageExtender() {
    stateCollector = CloudDebugProcessStateCollector.getInstance();
  }

  @Override
  public String additionalLogoutMessage() {
     if (stateCollector != null
        && stateCollector.getBackgroundListeningStates() != null
        && !stateCollector.getBackgroundListeningStates().isEmpty()) {
      return GctBundle.message("clouddebug.logout.additional.message");
    } else {
      return "";
    }
  }
}
