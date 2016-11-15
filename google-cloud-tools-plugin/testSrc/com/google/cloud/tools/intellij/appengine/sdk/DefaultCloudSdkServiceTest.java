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

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ProcessRunnerException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.intellij.flags.PropertiesFileFlagReader;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Path;
import java.util.Set;

/**
 * Unit tests for {@link DefaultCloudSdkService}
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCloudSdkServiceTest extends BasePluginTestCase {

  private static final CloudSdkVersion unsupportedVersion = new CloudSdkVersion("1.0.0");
  // arbitrarily high version number
  private static final CloudSdkVersion supportedVersion =
      new CloudSdkVersion(Integer.toString(Integer.MAX_VALUE) + ".0.0");

  private DefaultCloudSdkService service;

  @Mock
  private CloudSdk mockSdk;

  @Mock
  private Path mockPath;

  @Before
  public void setUp() throws Exception {
    service = new DefaultCloudSdkServiceForTesting();
  }

  @Test
  public void testGetMinimumRequiredCloudSdkVersion() {
    String expected = readRequiredCloudSdkVersion();
    assertEquals(new CloudSdkVersion(expected),
        DefaultCloudSdkService.getMinimumRequiredCloudSdkVersion());
  }

  @Test
  public void testValidateCloudSdk_cloudSdkNotFound() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenReturn(supportedVersion);
    doThrow(CloudSdkNotFoundException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = service.validateCloudSdk(mockPath);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_versionUnsupported() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenReturn(unsupportedVersion);
    Set<CloudSdkValidationResult> results = service.validateCloudSdk(mockPath);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED,
        results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_valid() throws ProcessRunnerException {
    when(mockSdk.getVersion()).thenReturn(supportedVersion);
    Set<CloudSdkValidationResult> results = service.validateCloudSdk(mockPath);
    assertEquals(0, results.size());
  }

  private String readRequiredCloudSdkVersion() {
    return new PropertiesFileFlagReader().getFlagString("cloudsdk.min.version");
  }

  // Create a special subclass of DefaultCloudSdkService so we can control some of its methods
  class DefaultCloudSdkServiceForTesting extends DefaultCloudSdkService {
    @Override
    CloudSdk buildCloudSdkWithPath(Path path) {
      return mockSdk;
    }
  }
}
