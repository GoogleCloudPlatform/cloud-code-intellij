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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.GctFeature;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkServiceManager;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkValidator;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Paths;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Unit tests for {@link AppEngineCloudConfigurable}. */
public final class AppEngineCloudConfigurableTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  @Mock @TestService private PluginInfoService mockPluginInfoService;

  @Mock @TestService private CloudSdkServiceManager mockCloudSdkServiceManager;
  @Mock @TestService private CloudSdkService mockCloudSdkService;

  @Mock @TestService private CloudSdkValidator mockCloudSdkValidator;

  @Before
  public void setUp() {
    when(mockCloudSdkServiceManager.getCloudSdkService()).thenReturn(mockCloudSdkService);
  }

  @Test
  public void managedSdk_feature_notEnabled_settingsVisible() {
    when(mockPluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(false);
    AppEngineCloudConfigurable appEngineCloudConfigurable = new AppEngineCloudConfigurable();

    assertThat(getAppEngineMoreInfoLabelText(appEngineCloudConfigurable))
        .contains("Go to Google -> Cloud SDK settings");
  }

  @Test
  public void managedSdk_feature_enabled_settingsNotVisible() {
    when(mockPluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(true);
    AppEngineCloudConfigurable appEngineCloudConfigurable = new AppEngineCloudConfigurable();

    assertThat(getAppEngineMoreInfoLabelText(appEngineCloudConfigurable))
        .doesNotContain("Go to Google -> Cloud SDK settings");
  }

  @Test
  public void managedSdk_feature_notEnabled_customSdk_validationLabel_present() {
    when(mockPluginInfoService.shouldEnable(GctFeature.MANAGED_SDK)).thenReturn(false);
    when(mockCloudSdkService.getSdkHomePath()).thenReturn(Paths.get("/some/sdk"));
    when(mockCloudSdkValidator.validateCloudSdk(any()))
        .thenReturn(ImmutableSet.of(CloudSdkValidationResult.CLOUD_SDK_NOT_FOUND));
    AppEngineCloudConfigurable appEngineCloudConfigurable = new AppEngineCloudConfigurable();

    assertThat(appEngineCloudConfigurable.getSdkValidationErrorLabel().isVisible()).isTrue();
    assertThat(appEngineCloudConfigurable.getSdkValidationErrorLabel().getText())
        .contains("Cloud SDK home directory is not specified.");
  }

  private String getAppEngineMoreInfoLabelText(AppEngineCloudConfigurable cloudConfigurable) {
    try {
      Document infoLabelDocument = cloudConfigurable.getAppEngineMoreInfoLabel().getDocument();
      return infoLabelDocument.getText(0, infoLabelDocument.getLength());
    } catch (BadLocationException e) {
      throw new AssertionError(e);
    }
  }
}
