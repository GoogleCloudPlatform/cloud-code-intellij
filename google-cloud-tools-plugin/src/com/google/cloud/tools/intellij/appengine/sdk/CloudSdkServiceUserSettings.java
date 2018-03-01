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
import com.google.common.base.Strings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ServiceManager;
import com.sun.istack.NotNull;

/** Stores user settings for {@link CloudSdkService}, including choice of implementation. */
class CloudSdkServiceUserSettings {
  private static CloudSdkServiceUserSettings instance;

  private static final CloudSdkServiceType DEFAULT_SDK_TYPE = CloudSdkServiceType.MANAGED_SDK;
  private static final boolean DEFAULT_MANAGED_SDK_AUTOMATIC_UPDATES = true;

  private static final String SDK_TYPE_PROPERTY_NAME = "SDK_TYPE_PROPERTY_NAME";
  private static final String CLOUD_SDK_PROPERTY_KEY = "GCT_CLOUD_SDK_HOME_PATH";
  private static final String SDK_AUTOMATIC_UPDATES_PROPERTY_NAME =
      "SDK_AUTOMATIC_UPDATES_PROPERTY_NAME";

  private PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

  static CloudSdkServiceUserSettings getInstance() {
    if (instance == null) {
      instance = new CloudSdkServiceUserSettings();
    }
    return instance;
  }

  @NotNull
  CloudSdkServiceType getUserSelectedSdkServiceType() {
    String sdkTypeName = propertiesComponent.getValue(SDK_TYPE_PROPERTY_NAME);
    CloudSdkServiceType sdkType;
    try {
      sdkType = CloudSdkServiceType.valueOf(Strings.nullToEmpty(sdkTypeName));
    } catch (Exception ex) {
      sdkType = DEFAULT_SDK_TYPE;
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
    propertiesComponent.setValue(SDK_AUTOMATIC_UPDATES_PROPERTY_NAME, enableAutomaticUpdates);
  }

  String getCustomSdkPath() {
    return propertiesComponent.getValue(CLOUD_SDK_PROPERTY_KEY);
  }

  void setCustomSdkPath(String path) {
    propertiesComponent.setValue(CLOUD_SDK_PROPERTY_KEY, path);
  }

  enum CloudSdkServiceType {
    MANAGED_SDK,
    CUSTOM_SDK
  }
}
