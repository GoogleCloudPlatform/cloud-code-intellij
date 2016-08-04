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

package com.google.cloud.tools.intellij.appengine.sdk;

import com.google.cloud.tools.intellij.appengine.util.CloudSdkUtil;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.util.text.StringUtil;

/**
 * Default implementation of {@link CloudSdkService} backed by {@link PropertiesComponent} for
 * serialization.
 */
public class DefaultCloudSdkService extends CloudSdkService {

  private PropertiesComponent propertiesComponent;
  private static final String CLOUD_SDK_PROPERTY_KEY = "GCT_CLOUD_SDK_HOME_PATH";

  public DefaultCloudSdkService() {
    this.propertiesComponent = PropertiesComponent.getInstance();
  }

  @Override
  public String getCloudSdkHomePath() {
    return propertiesComponent.getValue(CLOUD_SDK_PROPERTY_KEY);
  }

  @Override
  public void setCloudSdkHomePath(String cloudSdkHomePath) {
    if (!StringUtil.isEmpty(cloudSdkHomePath)
        && CloudSdkUtil.containsCloudSdkExecutable(cloudSdkHomePath)) {
      propertiesComponent.setValue(CLOUD_SDK_PROPERTY_KEY, cloudSdkHomePath);
    }
  }
}
