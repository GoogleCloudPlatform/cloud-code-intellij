package com.google.gct.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for defining the actual tracking behavior, implementations must be declared in plugin.xml
 * for the {@link UsageTrackerExtensionPointBean} extension point
 */
public interface UsageTracker {
  /**
   * When tracking events, do NOT include any information that can identify the user
   */
  void trackEvent(@NotNull String eventCategory,
      @NotNull String eventAction,
      @Nullable String eventLabel,
      @Nullable Integer eventValue);


}
