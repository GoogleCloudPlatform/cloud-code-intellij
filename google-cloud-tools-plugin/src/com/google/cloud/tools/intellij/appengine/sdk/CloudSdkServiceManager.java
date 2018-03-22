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

import java.util.Map;
import org.fest.util.Maps;

/** Manages current selection of {@link CloudSdkService} implementation. */
public class CloudSdkServiceManager {
  private final Map<CloudSdkServiceType, CloudSdkService> supportedCloudSdkServices;

  public CloudSdkServiceManager() {
    supportedCloudSdkServices = Maps.newHashMap();
    supportedCloudSdkServices.put(CloudSdkServiceType.CUSTOM_SDK, new DefaultCloudSdkService());
    supportedCloudSdkServices.put(CloudSdkServiceType.MANAGED_SDK, new ManagedCloudSdkService());
  }

  public CloudSdkService getCloudSdkService() {
    return supportedCloudSdkServices.get(
        CloudSdkServiceUserSettings.getInstance().getUserSelectedSdkServiceType());
  }

  /** Callback when a user selected and applied a new cloud sdk type. */
  public void onNewCloudSdkServiceTypeSelected(CloudSdkServiceType newServiceType) {
    if (supportedCloudSdkServices.containsKey(newServiceType)) {
      supportedCloudSdkServices.get(newServiceType).activate();
    } else {
      throw new UnsupportedCloudSdkTypeException(newServiceType.name());
    }
  }

  /** Callback interface to allow SDK precondition checks to communicate errors and log progress. */
  public interface CloudSdkPreconditionCheckCallback {
    void log(String message);

    void onError(String message);
  }

  private static class UnsupportedCloudSdkTypeException extends RuntimeException {
    private UnsupportedCloudSdkTypeException(String message) {
      super(message);
    }
  }
}
