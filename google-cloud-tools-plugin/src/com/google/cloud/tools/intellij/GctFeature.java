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

import com.google.cloud.tools.intellij.util.IntelliJPlatform;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The set of Google Cloud Tools features that we want to be able to conditionally enable.
 */
public enum GctFeature implements Feature {
  // DEBUGGER is enabled in IDEA Ultimate and Community, and disabled everywhere else.
  DEBUGGER(ImmutableSet.of(IntelliJPlatform.IDEA, IntelliJPlatform.IDEA_IC), null, null),
  APPENGINE_FLEX(null, "feature.appengine.flex", "ct4ij.feature.appengine.flex");

  private final Set<IntelliJPlatform> supportedPlatforms;
  private final String resourceFlagName;
  private final String systemFlagName;

  GctFeature(Set<IntelliJPlatform> supportedPlatforms, String resourceFlagName,
      String systemFlagName) {
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

