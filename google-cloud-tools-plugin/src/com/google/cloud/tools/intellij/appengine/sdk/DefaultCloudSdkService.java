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

/**
 * Default implementation of {@link CloudSdkService}.
 */
public class DefaultCloudSdkService extends CloudSdkService {

  private String cloudSdkHomePath;

  @Override
  public String getCloudSdkHomePath() {
    return cloudSdkHomePath;
  }

  @Override
  public void setCloudSdkHomePath(String cloudSdkHomePath) {
    this.cloudSdkHomePath = cloudSdkHomePath;
  }
}
