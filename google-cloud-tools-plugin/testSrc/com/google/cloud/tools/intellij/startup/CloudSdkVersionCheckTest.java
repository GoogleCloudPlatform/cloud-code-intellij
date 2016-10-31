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

package com.google.cloud.tools.intellij.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ProcessRunnerException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CloudSdkVersionCheckTest extends BasePluginTestCase {

  private CloudSdkVersionCheck checker;

  @Mock CloudSdkService cloudSdkServiceMock;
  @Mock CloudSdk cloudSdkMock;

  @Before
  public void setUp() throws ProcessRunnerException {
    registerService(CloudSdkService.class, cloudSdkServiceMock);

    when(cloudSdkMock.getVersion()).thenReturn(new CloudSdkVersion("129.0.0"));

    checker = new CloudSdkVersionCheck();
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_isSupported() {
    when(cloudSdkServiceMock.isCloudSdkVersionSupported(cloudSdkMock)).thenReturn(true);
    boolean result = checker.notifyIfCloudSdkNotSupported(cloudSdkMock);
    assertFalse(result);
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_notSupported() {
    when(cloudSdkServiceMock.isCloudSdkVersionSupported(cloudSdkMock)).thenReturn(false);
    when(cloudSdkServiceMock.getMinimumRequiredCloudSdkVersion()).thenReturn(new CloudSdkVersion("131.0.0"));
    boolean result = checker.notifyIfCloudSdkNotSupported(cloudSdkMock);
    assertTrue(result);
  }
}
