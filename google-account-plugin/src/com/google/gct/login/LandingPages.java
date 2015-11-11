package com.google.gct.login;

/**
 * Configures the correct Google login landing page based off of the IJ platform.
 */
public enum LandingPages {
  INTELLIJ(
      "https://cloud.google.com/tools/intellij/auth_success",
      "https://cloud.google.com/tools/intellij/auth_failure"
  ),
  ANDROID_STUDIO(
      "https://developers.google.com/cloud/mobile/auth_success",
      "https://developers.google.com/cloud/mobile/auth_failure"
  );

  private final String successPage;
  private final String failurePage;

  LandingPages(String successPage, String failurePage) {
    this.successPage = successPage;
    this.failurePage = failurePage;
  }

  public String getSuccessPage() {
    return successPage;
  }

  public String getFailurePage() {
    return failurePage;
  }
}
