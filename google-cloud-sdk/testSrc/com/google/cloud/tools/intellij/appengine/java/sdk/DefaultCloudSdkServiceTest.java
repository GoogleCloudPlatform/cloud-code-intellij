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

package com.google.cloud.tools.intellij.appengine.java.sdk;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.AppEngineJavaComponentsNotInstalledException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkOutOfDateException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Unit tests for {@link DefaultCloudSdkService} */
public class DefaultCloudSdkServiceTest {
  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService service;

  @Spy @TestService private CloudSdkValidator sdkValidator;

  @Mock @TestService private CloudSdkServiceManager cloudSdkServiceManager;

  @Mock private CloudSdk mockSdk;

  @Before
  public void setUp() throws Exception {
    when(service.getSdkHomePath()).thenReturn(Paths.get("/home/path"));
    when(cloudSdkServiceManager.getCloudSdkService()).thenReturn(service);
    doReturn(mockSdk).when(sdkValidator).buildCloudSdkWithPath(any(Path.class));
  }

  @Test
  public void testValidateCloudSdk_cloudSdkNotFound()
      throws CloudSdkNotFoundException, CloudSdkOutOfDateException, CloudSdkVersionFileException {
    doThrow(CloudSdkNotFoundException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk();
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
    assertFalse(sdkValidator.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_versionOutOfDate()
      throws CloudSdkNotFoundException, CloudSdkOutOfDateException, CloudSdkVersionFileException {
    doThrow(CloudSdkOutOfDateException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk();
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_MINIMUM_VERSION, results.iterator().next());
    assertFalse(sdkValidator.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_versionParseError()
      throws CloudSdkNotFoundException, CloudSdkOutOfDateException, CloudSdkVersionFileException {
    doThrow(CloudSdkVersionFileException.class).when(mockSdk).validateCloudSdk();
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk();
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_VERSION_FILE_ERROR, results.iterator().next());
    assertFalse(sdkValidator.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_valid() {
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk();
    assertEquals(0, results.size());
    assertTrue(sdkValidator.isValidCloudSdk());
  }

  @Test
  public void testValidateCloudSdk_nullPath() {
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk((Path) null);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_nullString() {
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk((String) null);
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_specialChars() {
    if (System.getProperty("os.name").contains("Windows")) {
      Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk(" /path");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(sdkValidator.isValidCloudSdk(" /path"));
      results = sdkValidator.validateCloudSdk("/path ");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(sdkValidator.isValidCloudSdk("/path "));
      results = sdkValidator.validateCloudSdk("/path/with/<special");
      assertEquals(1, results.size());
      assertEquals(CloudSdkValidationResult.MALFORMED_PATH, results.iterator().next());
      assertFalse(sdkValidator.isValidCloudSdk("/path/with/<special"));
    }
  }

  @Test
  public void testValidateCloudSdk_goodString() {
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk("/good/path");
    assertEquals(0, results.size());
    assertTrue(sdkValidator.isValidCloudSdk("/good/path"));
  }

  @Test
  public void testValidateJavaComponents() throws AppEngineJavaComponentsNotInstalledException {
    doThrow(AppEngineJavaComponentsNotInstalledException.class)
        .when(mockSdk)
        .validateAppEngineJavaComponents();
    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk("/good/path");
    assertEquals(1, results.size());
    assertEquals(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT, results.iterator().next());
  }

  @Test
  public void testValidateCloudSdk_multipleResults()
      throws AppEngineJavaComponentsNotInstalledException, CloudSdkNotFoundException,
          CloudSdkOutOfDateException, CloudSdkVersionFileException {
    doThrow(AppEngineJavaComponentsNotInstalledException.class)
        .when(mockSdk)
        .validateAppEngineJavaComponents();
    doThrow(CloudSdkOutOfDateException.class).when(mockSdk).validateCloudSdk();

    Set<CloudSdkValidationResult> results = sdkValidator.validateCloudSdk("/good/path");
    assertEquals(2, results.size());
    assertTrue(results.contains(CloudSdkValidationResult.NO_APP_ENGINE_COMPONENT));
    assertTrue(results.contains(CloudSdkValidationResult.CLOUD_SDK_NOT_MINIMUM_VERSION));
  }
}
