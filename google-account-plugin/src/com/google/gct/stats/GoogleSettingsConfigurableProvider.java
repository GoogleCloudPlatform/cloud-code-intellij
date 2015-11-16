package com.google.gct.stats;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Registers an implementation of {@code applicationConfigurable} extension to provide a
 * Google Cloud Tools tab in the "Settings" dialog if current application is IntelliJ.
 */
public class GoogleSettingsConfigurableProvider extends ConfigurableProvider {
    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new GoogleSettingsConfigurable();
    }

    /**
     * @return true if running platform is IntelliJ and false otherwise
     */
    @Override
    public boolean canCreateConfigurable() {
        // For now we can hide Google entirely if usage tracking isn't available as there are no
        // other Google related account settings in the IJ UI.
        if (PlatformUtils.isIntelliJ() && UsageTrackerManager.getInstance().isUsageTrackingAvailable()) {
            return true;
        } else {
            return false;
        }
    }
}
