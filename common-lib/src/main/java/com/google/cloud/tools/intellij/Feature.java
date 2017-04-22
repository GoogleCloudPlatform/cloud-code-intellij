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

import com.google.cloud.tools.intellij.util.IntelliJPlatform;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * An instance of this class represents a feature in one of our plugins. The sole purpose of a
 * {@code feature} is to determine whether to enable the corresponding functionality using the
 * {@link PluginInfoService} and the {@link PluginConfigurationService}. The algorithm that
 * determines whether a given {@code feature} configuration is enabled is as follows:
 *
 * <ol>
 *   <li>If the current platform the plugin is running in is contained in the list of {@link
 *       #getSupportedPlatforms()}, then the feature is enabled.
 *   <li>If the plugin's config.properties file contains a key with {@link #getResourceFlagName()}
 *       set to {@code true}, then the feature is enabled.
 *   <li>If there is a system environment variable named {@link #getSystemFlagName()} with a value
 *       of "true|TRUE", then it's enabled.
 *   <li>In all other cases, the feature is disabled.
 * </ol>
 */
public interface Feature {

  @Nullable
  Set<IntelliJPlatform> getSupportedPlatforms();

  @Nullable
  String getResourceFlagName();

  @Nullable
  String getSystemFlagName();
}
