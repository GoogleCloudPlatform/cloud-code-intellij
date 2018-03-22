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
import java.nio.file.Path;
import org.jetbrains.annotations.Nullable;

/** IntelliJ configured service for providing the path to the Cloud SDK. */
public interface CloudSdkService {

  /** Shortcut for getting currently active implementation of {@link CloudSdkService}. */
  static CloudSdkService getInstance() {
    return ServiceManager.getService(CloudSdkServiceManager.class).getCloudSdkService();
  }

  /** Called when this service becomes primary choice for serving Cloud SDK. */
  void activate();

  @Nullable
  Path getSdkHomePath();

  SdkStatus getStatus();

  /** Returns true if install is supported and started, false if install is not supported. */
  boolean supportsInstall();

  /** Asks SDK service to attempt install and prepare Cloud SDK if it's not ready for use yet. */
  void install();

  void addStatusUpdateListener(SdkStatusUpdateListener listener);

  void removeStatusUpdateListener(SdkStatusUpdateListener listener);

  enum SdkStatus {
    READY,
    INSTALLING,
    INVALID,
    NOT_AVAILABLE
  }

  /** Interface to receive SDK service updates like changes from installing to available. */
  interface SdkStatusUpdateListener {
    void onSdkStatusChange(CloudSdkService sdkService, SdkStatus status);
  }
}
