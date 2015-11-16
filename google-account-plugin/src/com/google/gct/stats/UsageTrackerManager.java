package com.google.gct.stats;

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.gct.login.PluginFlags;
import com.google.gct.login.PropertiesFilePluginFlags;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.Nullable;

/**
 * Stores the user's choice to opt in/out of sending usage metrics via the Google Usage Tracker.
 */
// TODO: Refactor to an IntelliJ service
public final class UsageTrackerManager {
  public static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";
  @VisibleForTesting
  protected static final String USAGE_TRACKER_PROPERTY_PLACEHOLDER = "${usageTrackerProperty}";
  private static UsageTrackerManager instance;
  private PropertiesComponent datastore;
  private PluginFlags flags;
  private static final Object factoryLock = new Object();

  @VisibleForTesting
  UsageTrackerManager() {
    datastore = PropertiesComponent.getInstance();
    flags = new PropertiesFilePluginFlags();
  }

  @VisibleForTesting
  UsageTrackerManager(PropertiesComponent propertiesComponent, PluginFlags flags) {
    this.datastore = propertiesComponent;
    this.flags = flags;
  }

  public static UsageTrackerManager getInstance() {
    synchronized (factoryLock) {
      if (instance == null) {
        instance = new UsageTrackerManager();
      }
      return instance;
    }
  }

  public void setTrackingPreference(boolean optIn) {
    datastore.setValue(USAGE_TRACKER_KEY, String.valueOf(optIn));
  }

  public boolean hasUserOptedIn() {
    return datastore.getBoolean(USAGE_TRACKER_KEY, false);
  }

  /**
   * Returns {@code true} if the user has opted in or out of usage tracking, and {@code false} if
   * the user has yet to indicate a tracking preference.
   */
  public boolean hasUserRecordedTrackingPreference() {
    return datastore.getValue(USAGE_TRACKER_KEY) != null;
  }

  /**
   * Indicates whether usage tracking is configured for this plugin's release and platform.  This is
   * independent of whether the user is opted in to usage tracking.
   *
   * {@code isUsageTrackingAvailable()} and {@link #hasUserOptedIn()} both need to return
   * {@code true} for tracking to be enabled. Call {@link #isTrackingEnabled()} to determine whether
   * to do user tracking.
   */
  public boolean isUsageTrackingAvailable() {
    return PlatformUtils.isIntelliJ() && (getAnalyticsProperty() != null);
  }

  @Nullable
  protected String getAnalyticsProperty() {
    String analyticsId = flags.getAnalyticsId();
    if (analyticsId != null && !USAGE_TRACKER_PROPERTY_PLACEHOLDER.equals(analyticsId)) {
      return analyticsId;
    }
    return null;
  }

  public boolean isTrackingEnabled() {
    return isUsageTrackingAvailable() && hasUserOptedIn();
  }
}
