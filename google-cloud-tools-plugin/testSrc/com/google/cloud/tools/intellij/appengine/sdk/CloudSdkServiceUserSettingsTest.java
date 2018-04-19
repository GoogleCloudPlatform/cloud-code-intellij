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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link CloudSdkServiceUserSettings} */
public class CloudSdkServiceUserSettingsTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private PluginInfoService pluginInfoService;

  private CloudSdkServiceUserSettings userSettings;

  @Before
  public void setUp() throws Exception {
    // enable managed SDK UI - remove when feature is rolled out.
    when(pluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(true);

    userSettings = new CloudSdkServiceUserSettings();
    // clear persisted values between unit test runs.
    CloudSdkServiceUserSettings.reset();
  }

  @Test
  public void emptySettings_managedSdk_selectedByDefault() {
    assertThat(userSettings.getUserSelectedSdkServiceType())
        .isEqualTo(CloudSdkServiceType.MANAGED_SDK);
  }

  @Test
  public void empty_sdkPath_managedSdk_selectedByDefault() {
    userSettings.setCustomSdkPath("");

    assertThat(userSettings.getUserSelectedSdkServiceType())
        .isEqualTo(CloudSdkServiceType.MANAGED_SDK);
  }

  @Test
  public void previous_sdkPath_exists_customSdk_type_selectedByDefault() {
    userSettings.setCustomSdkPath("/home/gcloud");

    assertThat(userSettings.getUserSelectedSdkServiceType())
        .isEqualTo(CloudSdkServiceType.CUSTOM_SDK);
  }

  @Test
  public void unset_lastUpdateTime_returns_empty() {
    assertThat(userSettings.getLastAutomaticUpdateTimestamp().isPresent()).isFalse();
  }

  @Test
  public void lastUpdateTime_properly_set() {
    long timestamp = System.currentTimeMillis();
    userSettings.setLastAutomaticUpdateTimestamp(timestamp);

    assertThat(userSettings.getLastAutomaticUpdateTimestamp().isPresent()).isTrue();
    assertThat(userSettings.getLastAutomaticUpdateTimestamp().get()).isEqualTo(timestamp);
  }
}
