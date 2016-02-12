package com.google.gct.login;

import com.google.gct.idea.debugger.CloudDebugProcessStateCollector;
import com.google.gct.idea.util.GctBundle;

import com.intellij.openapi.application.ApplicationManager;

public class IntelliJGoogleLoginMessageExtender implements GoogleLoginMessageExtender {

  private final CloudDebugProcessStateCollector stateCollector;

  public IntelliJGoogleLoginMessageExtender() {
    stateCollector = ApplicationManager.getApplication()
                                       .getComponent(CloudDebugProcessStateCollector.class);
  }

  @Override
  public String additionalLogoutMessage() {
    if (!stateCollector.getBackgroundListeningStates().isEmpty()) {
      return GctBundle.message("clouddebug.logout.additional.message");
    } else {
      return "";
    }
  }
}
