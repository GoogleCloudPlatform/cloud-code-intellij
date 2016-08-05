/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.intellij.ide.plugins.IdeaPluginDescriptor;

/**
 * Instances of this service correspond to a particular IntelliJ plugin and
 * provide information about the current configuration of the plugin.
 */
public interface PluginInfoService {

  /**
   * Returns the IntelliJ plugin ID as configured in the plugin.xml.
   */
  String getPluginId();

  /**
   * Returns the name of the plugin as defined externally (the unified name of the gcloud plugin
   * and its dependencies).
   */
  String getExternalPluginName();

  /**
   * Returns the user agent to use for interactions with Google APIs.
   */
  String getUserAgent();

  /**
   * Returns the version of this plugin.
   */
  String getPluginVersion();

  /**
   * Determines whether a given {@code feature} should be enabled for the
   * current run of the plugin.
   */
  boolean shouldEnable(Feature feature);

  /**
   * Determines whether Google Feedback error reporting should be enabled
   * for the current run of this plugin.
   */
  boolean shouldEnableErrorFeedbackReporting();

  /**
   * Determines whether the given plugin is installed.
   */
  boolean isPluginInstalled(String pluginId);

  /**
   * Returns the descriptor for the given plugin ID.
   */
  IdeaPluginDescriptor getPluginById(String pluginId);
}
