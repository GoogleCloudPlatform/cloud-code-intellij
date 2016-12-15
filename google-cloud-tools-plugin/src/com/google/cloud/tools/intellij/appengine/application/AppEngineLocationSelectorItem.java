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

package com.google.cloud.tools.intellij.appengine.application;

import com.google.api.services.appengine.v1.model.Location;

import com.intellij.ui.UI;
import com.intellij.ui.components.JBLabel;
import com.yourkit.util.Strings;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

/**
 * Represents a single GCP project ui.
 */
class AppEngineLocationSelectorItem {

  private final static String STANDARD_ENV_AVAILABLE_KEY = "standardEnvironmentAvailable";
  private final static String FLEXIBLE_ENV_AVAILABLE_KEY = "flexibleEnvironmentAvailable";

  private final Location location;

  public AppEngineLocationSelectorItem(@NotNull Location location) {
    this.location = location;

    // TODO(alexsloan) when b/33458530 is addressed, we can just use location.getLocationId()
    String locationIdLabel = location.getLabels().get("cloud.googleapis.com/region");
    if (Strings.isNullOrEmpty(location.getLocationId())
        && !Strings.isNullOrEmpty(locationIdLabel)) {
      location.setLocationId(locationIdLabel);
    }
  }

  @Override
  public String toString() {
    return location.getLocationId();
  }

  public Location getLocation() {
    return location;
  }

  public boolean isFlexSupported() {
    return parseMetadataBoolean(STANDARD_ENV_AVAILABLE_KEY, location.getMetadata());
  }

  public boolean isStandardSupported() {
    return parseMetadataBoolean(FLEXIBLE_ENV_AVAILABLE_KEY, location.getMetadata());
  }

  private boolean parseMetadataBoolean(String key, Map<String, Object> metadata) {
    if (!metadata.containsKey(key)) {
      return false;
    }
    Object val = metadata.get(key);
    if (val == null || !(val instanceof Boolean)) {
      return false;
    }
    return (Boolean) val;
  }
}
