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

package com.google.cloud.tools.intellij;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFile;
import com.google.cloud.tools.intellij.testing.TestService;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Created by eshaul on 7/25/17. */
public class CloudToolsFeedbackActionTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService sdkService;
  @TestFile(name = "testCloudSdk") private File testCloudSdk;

  @Test
  public void testGetDisplayableSdkVersion_whenSdkIsInvalid() {
    when(sdkService.getSdkHomePath()).thenReturn(testCloudSdk.toPath());
    assertEquals("", CloudToolsFeedbackAction.getCloudSdkVersion());
  }
}
