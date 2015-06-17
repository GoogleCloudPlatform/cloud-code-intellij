package com.google.gct.idea.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by appu on 5/27/15.
 */
public class UsageTrackerDummyImpl extends UsageTracker {

    @Override
    public void trackEvent(@NotNull String eventCategory, @NotNull String eventAction, @Nullable String eventLabel, @Nullable Integer eventValue) {
        // do nothing right now
    }

}
