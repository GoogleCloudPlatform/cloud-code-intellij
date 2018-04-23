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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import com.intellij.openapi.components.ServiceManager;

/**
 * Intellij-registered service for notifying the user if their Cloud SDK version is not supported.
 */
public abstract class CloudSdkVersionNotifier {

  /** Returns a registered instance of CloudSdkVersionNotifier. */
  public static CloudSdkVersionNotifier getInstance() {
    return ServiceManager.getService(CloudSdkVersionNotifier.class);
  }

  /** Notifies the user if the saved Cloud SDK is not supported. */
  public abstract void notifyIfUnsupportedVersion();
}
