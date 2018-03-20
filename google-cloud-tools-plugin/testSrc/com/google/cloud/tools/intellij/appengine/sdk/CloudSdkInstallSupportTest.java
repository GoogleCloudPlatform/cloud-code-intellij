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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatusUpdateListener;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.project.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

/** Tests for {@link CloudSdkInstallSupport}. */
public class CloudSdkInstallSupportTest {
  @Rule public  CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkServiceManager mockCloudSdkServiceManager;
  @Mock private CloudSdkService mockSdkService;

  private CloudSdkInstallSupport cloudSdkInstallSupport;

  @Before
  public void setUp() {
    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(mockSdkService);

    cloudSdkInstallSupport = new CloudSdkInstallSupport();
  }

  @Test
  public void installingSdk_then_readySdk_correctly_returnsStatus() {
    when(mockSdkService.getStatus()).thenReturn(SdkStatus.INSTALLING);
    SdkStatusUpdateListener listenerCaptor = ArgumentCaptor.forClass(SdkStatusUpdateListener.class)
        .capture();
    doNothing().when(mockSdkService).addStatusUpdateListener(
        listenerCaptor);

    cloudSdkInstallSupport.waitUntilCloudSdkInstalled(mock(Project.class));
  }
}