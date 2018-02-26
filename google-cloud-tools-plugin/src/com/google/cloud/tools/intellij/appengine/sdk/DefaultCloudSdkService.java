/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.common.base.Strings;
import com.intellij.ide.util.PropertiesComponent;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of {@link CloudSdkService} backed by {@link PropertiesComponent} for
 * serialization.
 */
public class DefaultCloudSdkService implements CloudSdkService {

  private PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
  private static final String CLOUD_SDK_PROPERTY_KEY = "GCT_CLOUD_SDK_HOME_PATH";

  @Override
  public void activate() {
    // TODO track event that custom SDK is activated and used.
  }

  @Nullable
  @Override
  public Path getSdkHomePath() {
    String sdkPath = propertiesComponent.getValue(CLOUD_SDK_PROPERTY_KEY);
    if (sdkPath != null) {
      // To let Windows users that persisted the old malformed path save a new one.
      // TODO(joaomartins): Delete this after a while so gets are faster.
      if (CloudSdkValidator.isMalformedCloudSdkPath(sdkPath)) {
        UsageTrackerProvider.getInstance().trackEvent(GctTracking.CLOUD_SDK_MALFORMED_PATH).ping();
        return null;
      }
      return Paths.get(sdkPath);
    }

    // Let common library auto-discover Cloud SDK's location.
    try {
      return new CloudSdk.Builder().build().getSdkPath();
    } catch (AppEngineException aee) {
      return null;
    }
  }

  @Override
  public void setSdkHomePath(String cloudSdkHomePath) {
    propertiesComponent.setValue(CLOUD_SDK_PROPERTY_KEY, cloudSdkHomePath);
  }

  @Override
  public SdkStatus getStatus() {
    String sdkPath = propertiesComponent.getValue(CLOUD_SDK_PROPERTY_KEY);
    if (Strings.isNullOrEmpty(sdkPath)) {
      return SdkStatus.NOT_AVAILABLE;
    }

    boolean malformedSdkPath = CloudSdkValidator.isMalformedCloudSdkPath(sdkPath);
    return malformedSdkPath ? SdkStatus.INVALID : SdkStatus.READY;
  }

  @Override
  public boolean install() {
    return false; /* not supported. */
  }

  @Override
  public void addStatusUpdateListener(SdkStatusUpdateListener listener) {
    /* not supported. */
  }

  @Override
  public void removeStatusUpdateListener(SdkStatusUpdateListener listener) {
    /* not supported. */
  }
}
