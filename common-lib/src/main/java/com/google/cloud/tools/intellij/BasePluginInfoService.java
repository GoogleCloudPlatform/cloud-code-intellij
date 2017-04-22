/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij;

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

/** Subclasses of this class will inherit a full implementation of the {@link PluginInfoService}. */
// Don't expose PluginId in this service's API as it has a private
// constructor and makes testing impossible.
public abstract class BasePluginInfoService implements PluginInfoService {

  private static final String PLUGIN_NAME_EXTERNAL = "gcloud-intellij";
  private final String userAgent;
  private final IdeaPluginDescriptor plugin;
  private final FlagReader flagReader;

  protected BasePluginInfoService(@NotNull String pluginUserAgentName, @NotNull String pluginId) {
    this(
        pluginUserAgentName,
        PluginManager.getPlugin(PluginId.getId(pluginId)),
        new PropertiesFileFlagReader());
  }

  @VisibleForTesting
  BasePluginInfoService(
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
