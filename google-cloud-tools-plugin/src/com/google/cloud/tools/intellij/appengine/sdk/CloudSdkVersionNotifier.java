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

import com.intellij.openapi.components.ServiceManager;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Intellij-registered service for notifying the user if their Cloud SDK version is not supported.
 */
public abstract class CloudSdkVersionNotifier {

  /**
   * Returns a registered instance of CloudSdkVersionNotifier.
   */
  public static CloudSdkVersionNotifier getInstance() {
    return ServiceManager.getService(CloudSdkVersionNotifier.class);
  }

  /**
   * Checks if the Cloud SDK at the given path is supported, and notifies the user if it is not
   * supported.
   */
  public abstract void notifyIfUnsupportedVersion(@NotNull Path cloudSdkPath);

}
