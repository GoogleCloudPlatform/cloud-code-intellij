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

import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;

public class CloudSdkUnsupportedVersionException extends Exception {

  private CloudSdkVersion requiredVersion;

  public CloudSdkUnsupportedVersionException(String message, CloudSdkVersion requiredVersion) {
    super(message);
    this.requiredVersion = requiredVersion;
  }

  public CloudSdkVersion getRequiredVersion() {
    return requiredVersion;
  }

}
