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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.managedcloudsdk.ManagedCloudSdk;
import com.google.cloud.tools.managedcloudsdk.UnsupportedOsException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Tests for {@link ManagedCloudSdkService} */
public class ManagedCloudSdkServiceTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Spy private ManagedCloudSdkService sdkService;

  @Mock private ManagedCloudSdk mockManagedCloudSdk;

  @Before
  public void setUp() throws UnsupportedOsException {
    doReturn(mockManagedCloudSdk).when(sdkService).createManagedSdk();
  }

  @Test
  public void initial_service_notActivated_status_notAvailable() {
    assertThat(sdkService.getStatus()).isEqualTo(SdkStatus.NOT_AVAILABLE);
  }
}
