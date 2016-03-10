package com.google.gct.login.util;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * Bundle class to get usage tracker messages from resources/messages
 */
public class TrackerMessageBundle {
    @NonNls
    private static final String BUNDLE_NAME = "messages.UsageTrackerBundle";
    private static Reference<ResourceBundle> bundleReference;

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(
            bundleReference);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            bundleReference = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    private TrackerMessageBundle() {
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    public static String getString(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
        return message(key, params);
    }
}
