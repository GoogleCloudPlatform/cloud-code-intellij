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

package com.google.cloud.tools.intellij.startup;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.java.sdk.CloudSdkServiceManager;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.service.ApplicationPluginInfoService;
import com.google.cloud.tools.intellij.service.PluginConfigurationService;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Tests to validate initialization on supported platforms */
@RunWith(MockitoJUnitRunner.class)
public class CloudToolsPluginInitializationComponentTest extends BasePluginTestCase {

  private static final String PLUGIN_ID_STRING = "com.google.gct.core";
  @Mock PluginInfoService pluginInfoService;
  @Mock PluginConfigurationService pluginConfigurationService;
  @Mock ActionManager actionManager;
  @Mock ApplicationPluginInfoService applicationInfoService;
  @Mock IntegratedGoogleLoginService googleLoginService;
  @Mock CloudSdkServiceManager cloudSdkServiceManager;

  CloudToolsPluginInitializationComponent testComponent;

  @Before
  public void registerMockServices() {
    registerService(PluginInfoService.class, pluginInfoService);
    registerService(PluginConfigurationService.class, pluginConfigurationService);
    registerService(ActionManager.class, actionManager);
    registerService(ApplicationPluginInfoService.class, applicationInfoService);
    registerService(IntegratedGoogleLoginService.class, googleLoginService);
    registerService(CloudSdkServiceManager.class, cloudSdkServiceManager);
    when(cloudSdkServiceManager.getCloudSdkService()).thenReturn(mock(CloudSdkService.class));

    testComponent = new CloudToolsPluginInitializationComponent();
  }

  @Test
  public void testInitComponent_feedbackIsEnabled() {
    when(pluginInfoService.shouldEnableErrorFeedbackReporting()).thenReturn(true);
    when(pluginInfoService.getPluginId()).thenReturn(PLUGIN_ID_STRING);
    testComponent.initComponent();
    verify(pluginConfigurationService).enabledGoogleFeedbackErrorReporting(PLUGIN_ID_STRING);
  }

  @Test
  public void testInitComponent_feedbackIsDisabled() {
    when(pluginInfoService.shouldEnableErrorFeedbackReporting()).thenReturn(false);
    testComponent.initComponent();
    verify(pluginConfigurationService, never()).enabledGoogleFeedbackErrorReporting(anyString());
  }

  @Test
  public void testInitComponent_usageTrackingIsEnabled() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(false);
    ApplicationManager.setApplication(mockApplication, mock(Disposable.class));
    testComponent = spy(new CloudToolsPluginInitializationComponent());
    doNothing().when(testComponent).configureUsageTracking();
    testComponent.initComponent();
    verify(testComponent).configureUsageTracking();
  }

  @Test
  public void testInitComponent_usageTrackingIsDisabled() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(true);
    ApplicationManager.setApplication(mockApplication, mock(Disposable.class));
    testComponent = spy(new CloudToolsPluginInitializationComponent());
    testComponent.initComponent();
    verify(testComponent, never()).configureUsageTracking();
  }

  @Test
  public void testInitComponent_loginServiceIsInitialized() {
    testComponent.initComponent();
    verify(googleLoginService).loadPersistedCredentials();
  }
}
