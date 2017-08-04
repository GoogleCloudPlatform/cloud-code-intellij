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

package com.google.cloud.tools.intellij.appengine.cloud;

import static com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.options.ConfigurationException;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineCloudConfigurable}. */
@RunWith(JUnit4.class)
public final class AppEngineCloudConfigurableTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private CloudSdkService mockCloudSdkService;

  private AppEngineCloudConfigurable appEngineCloudConfigurable;

  @Before
  public void setUp() {
    appEngineCloudConfigurable = new AppEngineCloudConfigurable();
  }

  @Test
  public void reset_withSdkPath_doesSetFieldText() {
    String sdkPath = "/some/sdk/path";
    when(mockCloudSdkService.getSdkHomePath()).thenReturn(Paths.get(sdkPath));

    appEngineCloudConfigurable.reset();

    assertThat(appEngineCloudConfigurable.getCloudSdkPanel().getCloudSdkDirectoryText())
        .isEqualTo(sdkPath);
  }

  @Test
  public void apply_withValidSdkPath_doesSetSdkPath() throws ConfigurationException {
    String sdkPath = "/some/sdk/path";
    appEngineCloudConfigurable.getCloudSdkPanel().setCloudSdkDirectoryText(sdkPath);

    appEngineCloudConfigurable.apply();

    verify(mockCloudSdkService).setSdkHomePath(sdkPath);
  }

  @Test
  public void apply_withInvalidSdkPath_doesSetSdkPath() throws ConfigurationException {
    String sdkPath = "/some/sdk/path";
    appEngineCloudConfigurable.getCloudSdkPanel().setCloudSdkDirectoryText(sdkPath);
    when(mockCloudSdkService.validateCloudSdk(anyString()))
        .thenReturn(ImmutableSet.of(CLOUD_SDK_NOT_FOUND));

    appEngineCloudConfigurable.apply();

    verify(mockCloudSdkService).setSdkHomePath(sdkPath);
  }
}
