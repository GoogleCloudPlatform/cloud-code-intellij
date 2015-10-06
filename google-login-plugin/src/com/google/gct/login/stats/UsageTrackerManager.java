package com.google.gct.login.stats;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Stores the users choice to opt in/out of sending usage metrics via the Google Usage Tracker.
 */
public class UsageTrackerManager {
    private static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";

    private UsageTrackerManager() {}

    public static void setOptIn(boolean optIn) {
        PropertiesComponent.getInstance().setValue(USAGE_TRACKER_KEY, String.valueOf(optIn));
    }

    public static boolean getOptIn() {
        return PropertiesComponent.getInstance().getBoolean(USAGE_TRACKER_KEY, false);
    }

    /**
     * @return true if the user has opted in/out of usage tracking; false otherwise
     */
    public static boolean isTrackingConfigured() {
        return PropertiesComponent.getInstance().getValue(USAGE_TRACKER_KEY) != null;
    }

}
