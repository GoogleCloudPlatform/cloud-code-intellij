package com.google.gct.login.stats;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Created by nbashirbello on 9/30/15.
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
