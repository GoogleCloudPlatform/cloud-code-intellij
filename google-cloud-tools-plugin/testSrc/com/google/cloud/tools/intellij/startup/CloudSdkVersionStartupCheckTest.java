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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkVersionNotifier;
import com.google.cloud.tools.intellij.appengine.java.startup.CloudSdkVersionStartupCheck;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.intellij.openapi.project.Project;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Unit tests for {@link CloudSdkVersionStartupCheck} */
@RunWith(MockitoJUnitRunner.class)
public final class CloudSdkVersionStartupCheckTest extends BasePluginTestCase {

  @Mock private Project mockProject;
  @Mock private CloudSdkVersionNotifier cloudSdkVersionNotifier;
  @Mock private CloudSdkService cloudSdkService;

  private CloudSdkVersionStartupCheck cloudSdkVersionStartupCheck;

  @Before
  public void setUp() {
    registerService(CloudSdkVersionNotifier.class, cloudSdkVersionNotifier);
    registerService(CloudSdkService.class, cloudSdkService);
    cloudSdkVersionStartupCheck = new CloudSdkVersionStartupCheck();
  }

  @Test
  public void testRunActivity() {
    cloudSdkVersionStartupCheck.runActivity(mockProject);

    verify(cloudSdkVersionNotifier, times(1)).notifyIfUnsupportedVersion();
  }
}
