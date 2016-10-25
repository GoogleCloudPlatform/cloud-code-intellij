/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The set of Google Cloud Tools features that we want to be able to conditionally enable.
 *
 * <p>Enum elements marked {@link Deprecated} are fully released and no longer hidden behind a flag.
 */
public enum GctFeature implements Feature {

  /**
   * Deprecating but not removing this feature as it serves as a useful sample and it's kinda nice
   * to see the {@code GctFeature} history of flags in this enum.
   */
  @Deprecated
  APPENGINE_FLEX(null, "feature.appengine.flex", "ct4ij.feature.appengine.flex");

  private final Set<IntelliJPlatform> supportedPlatforms;
  private final String resourceFlagName;
  private final String systemFlagName;

  /**
   * Constructs a feature flag using the specified configuration.
   *
   * <p>Somewhat counter-intuitively the {@code supportedPlatforms} set defines the platforms that
   * the feature is always on for, regardless of whether a flag is passed as a resource or system
   * property.
   *
   * <p>Look at {@link BasePluginInfoService#shouldEnable(Feature)} to review how this configuration
   * is checked.
   *
   * @param supportedPlatforms any platforms passed here will automatically enable this feature for
   *     all users of that platform
   * @param resourceFlagName a resource bundle property name that would be read for the flag value
   * @param systemFlagName a Java system property name that would be read for the flag value
   */
  GctFeature(
      Set<IntelliJPlatform> supportedPlatforms, String resourceFlagName, String systemFlagName) {
    this.supportedPlatforms = supportedPlatforms;
    this.resourceFlagName = resourceFlagName;
    this.systemFlagName = systemFlagName;
  }

  @Nullable
  @Override
  public Set<IntelliJPlatform> getSupportedPlatforms() {
    return supportedPlatforms;
  }

  @Nullable
  @Override
  public String getResourceFlagName() {
    return resourceFlagName;
  }

  @Nullable
  @Override
  public String getSystemFlagName() {
    return systemFlagName;
  }
}
