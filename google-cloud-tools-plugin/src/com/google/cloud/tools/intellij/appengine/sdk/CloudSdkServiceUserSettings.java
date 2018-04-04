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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Stores user settings for {@link CloudSdkService}, including choice of implementation. */
public class CloudSdkServiceUserSettings {
  private static CloudSdkServiceUserSettings instance;

  private static final CloudSdkServiceType DEFAULT_SDK_TYPE = CloudSdkServiceType.MANAGED_SDK;
  static final boolean DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES = true;

  private static final String SDK_TYPE_PROPERTY_NAME = "GCT_CLOUD_SDK_TYPE";
  private static final String CUSTOM_CLOUD_SDK_PATH_PROPERTY_NAME = "GCT_CLOUD_SDK_HOME_PATH";
  private static final String SDK_AUTOMATIC_UPDATES_PROPERTY_NAME =
      "GCT_CLOUD_SDK_AUTOMATIC_UPDATES";
  private static final String SDK_LAST_AUTOMATIC_UPDATE_TMESTAMP_PROPERTY_NAME =
      "GCT_CLOUD_SDK_LAST_AUTOMATIC_UPDATE_TMESTAMP";

  private PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

  public static CloudSdkServiceUserSettings getInstance() {
    if (instance == null) {
      instance = new CloudSdkServiceUserSettings();
    }
    return instance;
  }

  @VisibleForTesting
  static void reset() {
    getInstance().propertiesComponent.unsetValue(SDK_TYPE_PROPERTY_NAME);
    getInstance().propertiesComponent.unsetValue(CUSTOM_CLOUD_SDK_PATH_PROPERTY_NAME);
    getInstance().propertiesComponent.unsetValue(SDK_AUTOMATIC_UPDATES_PROPERTY_NAME);
    getInstance().propertiesComponent.unsetValue(SDK_LAST_AUTOMATIC_UPDATE_TMESTAMP_PROPERTY_NAME);
  }

  @NotNull
  CloudSdkServiceType getUserSelectedSdkServiceType() {
    String sdkTypeName = propertiesComponent.getValue(SDK_TYPE_PROPERTY_NAME);
    CloudSdkServiceType sdkType;
    try {
      sdkType = CloudSdkServiceType.valueOf(Strings.nullToEmpty(sdkTypeName));
    } catch (Exception ex) {
      sdkType = DEFAULT_SDK_TYPE;
      // sdk type is unset - probably previous version of the SDK support didn't have it.
      // check for custom SDK path and use custom if it's set.
      if (!Strings.isNullOrEmpty(getCustomSdkPath())) {
        sdkType = CloudSdkServiceType.CUSTOM_SDK;
      }
    }

    // override result based on feature status until feature is done.
    if (!ServiceManager.getService(PluginInfoService.class).shouldEnable(GctFeature.MANAGED_SDK)) {
      sdkType = CloudSdkServiceType.CUSTOM_SDK;
    }

    return sdkType;
  }

  void setUserSelectedSdkServiceType(@NotNull CloudSdkServiceType cloudSdkServiceType) {
    propertiesComponent.setValue(SDK_TYPE_PROPERTY_NAME, cloudSdkServiceType.name());
  }

  boolean getEnableAutomaticUpdates() {
    return propertiesComponent.getBoolean(
        SDK_AUTOMATIC_UPDATES_PROPERTY_NAME, DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES);
  }

  void setEnableAutomaticUpdates(boolean enableAutomaticUpdates) {
    propertiesComponent.setValue(
        SDK_AUTOMATIC_UPDATES_PROPERTY_NAME,
        enableAutomaticUpdates,
        DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES /* need to specify default to avoid removal */);
  }

  long getLastAutomaticUpdateTimestamp() {
    return Optional.ofNullable(
            Longs.tryParse(
                Strings.nullToEmpty(
                    propertiesComponent.getValue(
                        SDK_LAST_AUTOMATIC_UPDATE_TMESTAMP_PROPERTY_NAME))))
        .orElse(0L);
  }

  void setLastAutomaticUpdateTimestamp(long timestamp) {
    propertiesComponent.setValue(
        SDK_LAST_AUTOMATIC_UPDATE_TMESTAMP_PROPERTY_NAME,
        Long.toString(timestamp),
        null /* null default not to remove property value. */);
  }

  @VisibleForTesting
  public String getCustomSdkPath() {
    return propertiesComponent.getValue(CUSTOM_CLOUD_SDK_PATH_PROPERTY_NAME);
  }

  public void setCustomSdkPath(String path) {
    propertiesComponent.setValue(
        CUSTOM_CLOUD_SDK_PATH_PROPERTY_NAME, path, null /* default for path */);
  }
}
