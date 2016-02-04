package com.google.gct.login;

import com.google.gct.idea.util.GctBundle;

public class IntelliJGoogleLoginMessageExtender implements GoogleLoginMessageExtender {

  @Override
  public String additionalLogoutMessage() {
    return GctBundle.message("clouddebug.logout.additional.message");
  }
}
