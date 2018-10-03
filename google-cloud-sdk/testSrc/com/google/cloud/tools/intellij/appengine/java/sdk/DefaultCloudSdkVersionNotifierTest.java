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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkNotFoundException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkVersionFileException;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.Sets;
import java.util.HashSet;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

/** Unit tests for {@link DefaultCloudSdkVersionNotifier} */
public class DefaultCloudSdkVersionNotifierTest {

  @Rule public CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  // Wrap the class under test in a spy so we can perform verifications on it
  @Spy private DefaultCloudSdkVersionNotifier checker;

  @Mock @TestService private CloudSdkService cloudSdkServiceMock;

  @Mock @TestService private CloudSdkValidator cloudSdkValidator;

  @Mock private CloudSdk cloudSdk;

  @Test
  public void testNotifyIfCloudSdkNotSupported_isSupported() {
    when(cloudSdkValidator.validateCloudSdk()).thenReturn(new HashSet<>());
    checker.notifyIfVersionOutOfDate();

    verify(checker, times(0)).showNotification(anyString(), anyString());
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_versionOutOfDateError() {
    when(cloudSdkValidator.validateCloudSdk())
        .thenReturn(Sets.newHashSet(CloudSdkValidationResult.CLOUD_SDK_NOT_MINIMUM_VERSION));

    checker.notifyIfVersionOutOfDate();
    verify(checker, times(1))
        .showNotification(
            "Google Cloud SDK Update Required",
            "<p>The Cloud SDK is out of date. Version "
                + CloudSdk.MINIMUM_VERSION
                + " is the minimum required version for use with the "
                + "Google Cloud Tools Plugin. To update, run \"gcloud components update\".</p>");
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_versionParseError()
      throws CloudSdkNotFoundException, CloudSdkVersionFileException {
    when(cloudSdkValidator.buildCloudSdk()).thenReturn(cloudSdk);
    when(cloudSdk.getVersion()).thenThrow(new CloudSdkVersionFileException("file error"));

    checker.notifyIfVersionParseError();
    verify(checker, times(1))
        .showNotification(
            "Unrecognized Cloud SDK Version",
            "<p>Operations may have unintended "
                + "results. You can install the Cloud SDK manually and set the path "
                + "via:<p><p>Settings -> Google -> Cloud Sdk -> Use a custom local "
                + "installation</p>");
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_sdkNotFound() {
    when(cloudSdkValidator.validateCloudSdk())
        .thenReturn(Sets.newHashSet(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));

    checker.notifyIfVersionOutOfDate();
    verify(checker, times(0)).showNotification(anyString(), anyString());
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_nullSdkPath() {
    checker.notifyIfVersionOutOfDate();
    verify(checker, times(0)).showNotification(anyString(), anyString());
  }
}
