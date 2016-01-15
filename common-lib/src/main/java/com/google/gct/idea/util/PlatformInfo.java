package com.google.gct.idea.util;

import com.google.common.collect.ImmutableSet;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class PlatformInfo {

  private static volatile String userAgent = null;

  public static final Set<String> SUPPORTED_PLATFORMS =
      ImmutableSet.of(PlatformUtils.IDEA_CE_PREFIX, PlatformUtils.IDEA_PREFIX);

  @NotNull
  public static IntelliJPlatform getCurrentPlatform() {
    return IntelliJPlatform.fromPrefix(PlatformUtils.getPlatformPrefix());
  }

  @NotNull
  public static String getCurrentPlatformName() {
    return getCurrentPlatform().getName();
  }

  @NotNull
  public static String getUserAgent() {
    if (userAgent != null) {
      return userAgent;
    }
    // todo read version from build.gradle
    String newUserAgent = "GCP-Plugin-for-IntelliJ/0.91";
    try {
      newUserAgent += " " + getCurrentPlatformName()
          + "/" + ApplicationInfo.getInstance().getBuild();
    } catch (IllegalArgumentException ex) {
      // ApplicationInfo.getInstance() doesn't work reliably in unit tests.
    }
    synchronized(PlatformInfo.class) {
      if (userAgent != null) {
        userAgent = newUserAgent;
      }
    }
    return newUserAgent;
  }
}
