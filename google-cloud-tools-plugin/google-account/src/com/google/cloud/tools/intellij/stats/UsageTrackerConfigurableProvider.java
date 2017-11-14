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

package com.google.cloud.tools.intellij.stats;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.Nullable;

/**
 * Registers an implementation of {@code applicationConfigurable} extension to provide a Google.
 * Cloud Tools tab in the "Settings" dialog if current application is IntelliJ.
 */
public class UsageTrackerConfigurableProvider extends ConfigurableProvider {

  @Nullable
  @Override
  public Configurable createConfigurable() {
    return new UsageTrackerConfigurable();
  }

  /**
   * @return true if running platform is IntelliJ and false otherwise.
   */
  @Override
  public boolean canCreateConfigurable() {
    // For now we can hide Google entirely if usage tracking isn't available as there are no
    // other Google related account settings in the IJ UI.
    // Create a sub-menu item for the cloud SDK and hide the usage tracker if not avaible
    return PlatformUtils.isIntelliJ() && UsageTrackerManager.getInstance()
        .isUsageTrackingAvailable();
  }
}
