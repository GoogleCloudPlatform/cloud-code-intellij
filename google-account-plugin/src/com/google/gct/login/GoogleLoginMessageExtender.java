package com.google.gct.login;


import com.intellij.openapi.extensions.ExtensionPointName;

public interface GoogleLoginMessageExtender {
  ExtensionPointName<GoogleLoginMessageExtender> EP_NAME = new ExtensionPointName<GoogleLoginMessageExtender>("com.google.gct.login.googleLoginMessageExtender");

  String additionalLogoutMessage();
}
