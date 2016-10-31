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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ProcessRunnerException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCloudSdkServiceTest extends BasePluginTestCase {

  private CloudSdkService service;

  @Mock
  private CloudSdk mockSdk;

  @Before
  public void setUp() throws Exception {
    service = new DefaultCloudSdkService();
  }

  @Test
  public void testIsCloudSdkSupported_priorVersion() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenReturn(new CloudSdkVersion("120.0.0"));
    assertFalse(service.isCloudSdkVersionSupported(mockSdk));
  }

  @Test
  public void testIsCloudSdkSupported_laterVersion() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenReturn(new CloudSdkVersion("600.1.1"));
    assertTrue(service.isCloudSdkVersionSupported(mockSdk));
  }

  @Test
  public void testIsCloudSdkSupported_gcloudException() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenThrow(ProcessRunnerException.class);
    assertFalse(service.isCloudSdkVersionSupported(mockSdk));
  }

  @Test
  public void testGetMinimumRequiredCloudSdkVersion() {
    assertNotNull(service.getMinimumRequiredCloudSdkVersion());
  }

}
