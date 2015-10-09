package com.google.gct.login.idea;

import com.google.gct.idea.feedback.FeedbackUtil;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Performs runtime initialization for the Google Login plugin.
 */
public class PluginInitializationComponent implements ApplicationComponent {

  private static final String PLUGIN_ID = "com.google.gct.login";

  @NotNull
  @Override
  public String getComponentName() {
    return "GoogleLogin.InitializationComponent";
  }

  @Override
  public void initComponent() {
    if (!"AndroidStudio".equals(PlatformUtils.getPlatformPrefix())) {
      FeedbackUtil.enableGoogleFeedbackErrorReporting(PLUGIN_ID);
    }
  }

  @Override
  public void disposeComponent() {
    // no-op
  }
}
