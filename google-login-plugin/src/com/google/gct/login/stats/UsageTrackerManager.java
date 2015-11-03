package com.google.gct.login.stats;

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

/**
 * Stores the user's choice to opt in/out of sending usage metrics via the Google Usage Tracker.
 */
public class UsageTrackerManager {
    private static final Logger LOG = Logger.getInstance(UsageTrackerManager.class);
    public static final String USAGE_TRACKER_KEY = "GOOGLE_CLOUD_TOOLS_USAGE_TRACKER_OPT_IN";
    private static final String USAGE_TRACKER_PROPERTY = "usage.tracker.property";
    private static final String USAGE_TRACKER_PROPERTY_PLACEHOLDER = "@usage.tracker.property@";
    private PropertiesComponent datastore;

    public UsageTrackerManager() {
        datastore = PropertiesComponent.getInstance();
    }

    @VisibleForTesting
    public UsageTrackerManager(PropertiesComponent propertiesComponent) {
        this.datastore = propertiesComponent;
    }

    public void setTrackingPreference(boolean optIn) {
        datastore.setValue(USAGE_TRACKER_KEY, String.valueOf(optIn));
    }

    public boolean getTrackingPreference() {
        return datastore.getBoolean(USAGE_TRACKER_KEY, false);
    }

    /**
     * @return true if the user has opted in/out of usage tracking; false otherwise
     */
    public boolean hasUserRecordedTrackingPreference() {
        return datastore.getValue(USAGE_TRACKER_KEY) != null;
    }

    /**
     * @return true if running on IntelliJ platform and the usage tracking property exists;
     * false otherwise
     */
    public boolean isUsageTrackingAvailable() {
        if (!PlatformUtils.isIntelliJ() || (getAnalyticsProperty() == null)) {
            return false;
        }
        return true;
    }

    @Nullable
    protected String getAnalyticsProperty() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/config.properties"));
            return properties.getProperty(USAGE_TRACKER_PROPERTY);
        } catch (IllegalArgumentException ex) {
            LOG.error(ex.getMessage());
            return null;
        } catch (NullPointerException ex) {
            LOG.error(ex.getMessage());
            return null;
        } catch (SecurityException ex) {
            LOG.error(ex.getMessage());
            return null;
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            return null;
        }

    }
}
