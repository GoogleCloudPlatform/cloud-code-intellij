package com.google.gct.login.stats;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Registers an implementation of {@code applicationConfigurable} extension to provide a
 * Google Cloud Tools tab in the "Settings" dialog if current application is IntelliJ.
 */
public class GctConfigurableProvider extends ConfigurableProvider {
    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new GctConfigurable();
    }

    /**
     * @return true if running platform is IntelliJ and false otherwise
     */
    @Override
    public boolean canCreateConfigurable() {
        if (PlatformUtils.isIntelliJ()) {
            return true;
        } else {
            return false;
        }
    }
}
