package com.google.gct.idea.feedback;

import com.intellij.ExtensionPoints;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.PluginId;

/**
 * Handy plugin related methods for Google Feedback integration.
 */
public final class FeedbackUtil {

  private FeedbackUtil() {}

  public static void enableGoogleFeedbackErrorReporting(String pluginId) {
    GoogleFeedbackErrorReporter errorReporter = new GoogleFeedbackErrorReporter();
    errorReporter
        .setPluginDescriptor(PluginManager.getPlugin(PluginId.getId(pluginId)));
    Extensions.getRootArea().getExtensionPoint(ExtensionPoints.ERROR_HANDLER_EP)
        .registerExtension(errorReporter);
  }
}
