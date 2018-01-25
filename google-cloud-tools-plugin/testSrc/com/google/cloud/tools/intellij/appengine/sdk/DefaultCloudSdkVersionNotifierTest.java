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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.Sets;
import com.intellij.util.containers.HashSet;
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

  @Test
  public void testNotifyIfCloudSdkNotSupported_isSupported() {
    when(cloudSdkValidator.validateCloudSdk()).thenReturn(new HashSet<>());
    checker.notifyIfUnsupportedVersion();

    verify(checker, times(0)).showNotification();
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_notSupported() {
    when(cloudSdkValidator.validateCloudSdk())
        .thenReturn(Sets.newHashSet(CloudSdkValidationResult.CLOUD_SDK_VERSION_NOT_SUPPORTED));

    checker.notifyIfUnsupportedVersion();
    verify(checker, times(1)).showNotification();
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_sdkNotFound() {
    when(cloudSdkValidator.validateCloudSdk())
        .thenReturn(Sets.newHashSet(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));

    checker.notifyIfUnsupportedVersion();
    verify(checker, times(0)).showNotification();
  }

  @Test
  public void testNotifyIfCloudSdkNotSupported_nullSdkPath() {
    checker.notifyIfUnsupportedVersion();
    verify(checker, times(0)).showNotification();
  }
}
