package com.google.gct.login;

import com.google.gct.idea.flags.FlagReader;
import com.google.gct.idea.flags.PropertiesFileFlagReader;

import com.intellij.openapi.diagnostic.Logger;

/**
 * This is a standard {@link PluginFlags} implementation that reads its values from the default
 * properties file.
 */
public class PropertiesFilePluginFlags implements PluginFlags {
  private static final Logger LOGGER = Logger.getInstance(PropertiesFilePluginFlags.class);
  private static final String USAGE_TRACKER_PROPERTY = "usage.tracker.property";
  private FlagReader flagReader;

  public PropertiesFilePluginFlags() {
    try {
      flagReader = new PropertiesFileFlagReader();
    } catch (IllegalArgumentException e) {
      // TODO: Fail hard once we refactor to services and using aswb service mockable tests
      LOGGER.warn(e);
    }
  }

  @Override
  public String getAnalyticsId() {
    return flagReader.getFlagString(USAGE_TRACKER_PROPERTY);
  }
}
