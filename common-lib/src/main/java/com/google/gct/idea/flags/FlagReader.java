package com.google.gct.idea.flags;

import org.jetbrains.annotations.Nullable;

/**
 * Generic interface for retrieving flag values for your plugin.
 */
public interface FlagReader {

  @Nullable
  String getFlagString(String propertyName);
}
