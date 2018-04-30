/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.service;

import com.google.cloud.tools.intellij.Feature;
import com.google.cloud.tools.intellij.flags.FlagReader;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.util.IntelliJPlatform;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.util.PlatformUtils;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/** Provides useful metadata about the Google Cloud Tools plugin. */
// Don't expose PluginId in this service's API as it has a private
// constructor and makes testing impossible.
public class DefaultPluginInfoService implements PluginInfoService {

  private static final String CLIENT_VERSION_PREFIX = "google.com/intellij/v";
  private static final String PLUGIN_NAME_EXTERNAL = "gcloud-intellij";

  private final String userAgent;
  private final IdeaPluginDescriptor plugin;
  private final FlagReader flagReader;

  public DefaultPluginInfoService() {
    this(
        "gcloud-intellij-cloud-tools-plugin",
        PluginManager.getPlugin(PluginId.getId("com.google.gct.core")),
        new PropertiesFileFlagReader());
  }

  @VisibleForTesting
  DefaultPluginInfoService(
      @NotNull String pluginUserAgentName,
      @NotNull IdeaPluginDescriptor plugin,
      @NotNull FlagReader flagReader) {
    this.plugin = plugin;
    this.userAgent = constructUserAgent(pluginUserAgentName, plugin.getVersion());
    this.flagReader = flagReader;
  }

  @Override
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * TODO(patflynn): Figure out if this is necessary. {@code userAgent} contains this information.
   */
  @Override
  public String getClientVersionForCloudDebugger() {
    return CLIENT_VERSION_PREFIX + getPluginVersion();
  }

  @Override
  public String getPluginVersion() {
    return plugin.getVersion();
  }

  @Override
  public String getPluginId() {
    return plugin.getPluginId().getIdString();
  }

  @Override
  public String getExternalPluginName() {
    return PLUGIN_NAME_EXTERNAL;
  }

  @Override
  public boolean shouldEnableErrorFeedbackReporting() {
    return !getCurrentPlatform().equals(IntelliJPlatform.ANDROID_STUDIO);
  }

  /**
   * !!!Please update {@link Feature}'s class documentation if you change the algorithm that
   * determines whether a feature is enabled.!!!
   */
  @Override
  public boolean shouldEnable(Feature feature) {
    Set<IntelliJPlatform> supportedPlatforms = feature.getSupportedPlatforms();
    if (supportedPlatforms != null && supportedPlatforms.contains(getCurrentPlatform())) {
      return true;
    }
    if (Boolean.parseBoolean(flagReader.getFlagString(feature.getResourceFlagName()))) {
      return true;
    }
    String flagName = feature.getSystemFlagName();
    if (flagName != null && Boolean.getBoolean(flagName)) {
      return true;
    }
    return false;
  }

  @VisibleForTesting
  String constructUserAgent(String pluginUserAgentName, String pluginVersion) {
    return pluginUserAgentName
        + "/"
        + pluginVersion
        + " ("
        + getCurrentPlatform()
        + "/"
        + getCurrentPlatformVersion()
        + ")";
  }

  @NotNull
  @VisibleForTesting
  IntelliJPlatform getCurrentPlatform() {
    return IntelliJPlatform.fromPrefix(PlatformUtils.getPlatformPrefix());
  }

  @NotNull
  @VisibleForTesting
  String getCurrentPlatformVersion() {
    return ApplicationInfo.getInstance().getBuild().asString();
  }
}
