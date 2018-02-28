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

import com.google.common.base.Strings;
import com.intellij.ide.util.PropertiesComponent;
import com.sun.istack.NotNull;

/** Stores user settings for {@link CloudSdkService}, including choice of implementation. */
class CloudSdkServiceUserSettings {
  private static CloudSdkServiceUserSettings instance;

  private static final CloudSdkServiceType DEFAULT_SDK_TYPE = CloudSdkServiceType.MANAGED_SDK;

  private static final String SDK_TYPE_PROPERTY_NAME = "SDK_TYPE_PROPERTY_NAME";

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

    return sdkType;
  }

  void setUserSelectedSdkServiceType(@NotNull CloudSdkServiceType cloudSdkServiceType) {
    propertiesComponent.setValue(SDK_TYPE_PROPERTY_NAME, cloudSdkServiceType.name());
  }

  enum CloudSdkServiceType {
    MANAGED_SDK,
    CUSTOM_SDK
  }
}
