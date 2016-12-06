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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.appengine.cloudsdk.serialization.CloudSdkVersion;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  public void testValidateCloudSdk_cloudSdkNotFound() throws  IOException {
    when(mockSdk.getVersion()).thenReturn(supportedVersion);
    doThrow(CloudSdkNotFoundException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = service.validateCloudSdk();
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
    assertFalse(service.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_versionUnsupported() throws IOException {
    doThrow(CloudSdkOutOfDateException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = service.validateCloudSdk();
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED,
        results.iterator().next());
    assertFalse(service.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_valid() throws IOException {
    Set<CloudSdkValidationResult> results = service.validateCloudSdk();
    assertEquals(0, results.size());
    assertTrue(service.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_nullPath() {
    Set<CloudSdkValidationResult> results = service.validateCloudSdk((Path) null);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_nullString() {
    Set<CloudSdkValidationResult> results = service.validateCloudSdk((String) null);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_specialChars() {
    if (System.getProperty("os.name").contains("Windows")) {
      Set<CloudSdkValidationResult> results = service.validateCloudSdk(" /path");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(service.isValidCloudSdk(" /path"));
      results = service.validateCloudSdk("/path ");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(service.isValidCloudSdk("/path "));
      results = service.validateCloudSdk("/path/with/<special");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(service.isValidCloudSdk("/path/with/<special"));
    }
  }

  @Test
  public void testValidateCloudSdk_goodString() throws IOException {
    Set<CloudSdkValidationResult> results = service.validateCloudSdk("/good/path");
    assertEquals(0, results.size());
    assertTrue(service.isValidCloudSdk("/good/path"));
  }

  @Test
  public void testValidateJavaComponents() throws IOException {
    doThrow(AppEngineJavaComponentsNotInstalledException.class).when(mockSdk)
        .validateAppEngineJavaComponents();
    Set<CloudSdkValidationResult> results = service.validateCloudSdk("/good/path");
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_multipleResults() throws IOException {
    doThrow(AppEngineJavaComponentsNotInstalledException.class).when(mockSdk)
        .validateAppEngineJavaComponents();
    doThrow(CloudSdkOutOfDateException.class).when(mockSdk).validateCloudSdk();

    Set<CloudSdkValidationResult> results = service.validateCloudSdk("/good/path");
    assertEquals(2, results.size());
    assertTrue(results.contains(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT));
    assertTrue(results.contains(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED));
  }

  // Create a special subclass of DefaultCloudSdkService so we can control some of its methods
  class DefaultCloudSdkServiceForTesting extends DefaultCloudSdkService {
    @Override
    CloudSdk buildCloudSdkWithPath(Path path) {
      return mockSdk;
    }

    @Nullable
    @Override
    public Path getSdkHomePath() {
      return Paths.get("/home/path");
    }
  }
}
