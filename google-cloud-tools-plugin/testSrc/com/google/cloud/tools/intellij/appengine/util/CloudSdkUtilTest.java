/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.google.cloud.tools.intellij.util.SystemEnvironmentProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class CloudSdkUtilTest extends BasePluginTestCase {

  @Mock
  private SystemEnvironmentProvider environmentProvider;

  private static final String CLOUD_SDK_EXECUTABLE_PATH
      = new File(String.format("/a/b/c/gcloud-sdk/bin/%s", CloudSdkUtil.getSystemCommand())).getAbsolutePath();
  private static final String CLOUD_SDK_DIR_PATH = new File("/a/b/c/gcloud-sdk").getAbsolutePath();

  @Before
  public void setUp() {
    registerService(SystemEnvironmentProvider.class, environmentProvider);
  }

  @Test
  public void testPaths() {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(new File(CLOUD_SDK_EXECUTABLE_PATH));

    assertEquals(CLOUD_SDK_EXECUTABLE_PATH,
        CloudSdkUtil.findCloudSdkExecutablePath(environmentProvider));
    assertEquals(CLOUD_SDK_DIR_PATH,
        CloudSdkUtil.findCloudSdkDirectoryPath(environmentProvider));
  }

  @Test
  public void testNullPaths() {
    when(environmentProvider.findInPath(anyString()))
        .thenReturn(null);

    assertEquals(null, CloudSdkUtil.findCloudSdkExecutablePath(environmentProvider));
    assertEquals(null, CloudSdkUtil.findCloudSdkDirectoryPath(environmentProvider));
  }

  @Test
  public void testDirExecutableConversion() {
    assertEquals(CLOUD_SDK_EXECUTABLE_PATH,
        CloudSdkUtil.toExecutablePath(CLOUD_SDK_DIR_PATH));
    assertEquals(CLOUD_SDK_DIR_PATH,
        CloudSdkUtil.toSdkHomeDirectory(CLOUD_SDK_EXECUTABLE_PATH));
  }
}
