package com.google.gct.idea.stats;

import com.intellij.openapi.components.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by appu on 5/27/15.
 */
public abstract class UsageTracker {
    /**
     * When using the usage tracker, do NOT include any information that can identify the user
     */
    @NotNull
    public static UsageTracker getInstance() {
        return ServiceManager.getService(UsageTracker.class);
    }

    /**
     * When tracking events, do NOT include any information that can identify the user
     */
    public abstract void trackEvent(@NotNull String eventCategory,
                                    @NotNull String eventAction,
                                    @Nullable String eventLabel,
                                    @Nullable Integer eventValue);
}
