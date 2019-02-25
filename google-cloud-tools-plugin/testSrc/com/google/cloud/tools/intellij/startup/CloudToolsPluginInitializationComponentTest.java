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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.analytics.UsageTrackingManagementService;
import com.google.cloud.tools.intellij.login.IntegratedGoogleLoginService;
import com.google.cloud.tools.intellij.service.ApplicationPluginInfoService;
import com.google.cloud.tools.intellij.service.PluginConfigurationService;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests to validate initialization on supported platforms */
public class CloudToolsPluginInitializationComponentTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);

  private static final String PLUGIN_ID_STRING = "com.google.gct.core";
  @Mock Disposable disposable;
  @Mock @TestService PluginInfoService pluginInfoService;
  @Mock @TestService PluginConfigurationService pluginConfigurationService;
  @Mock @TestService ApplicationPluginInfoService applicationInfoService;
  @Mock @TestService IntegratedGoogleLoginService googleLoginService;
  @Mock @TestService UsageTrackingManagementService usageTrackingManagementService;

  private Application application;
  CloudToolsPluginInitializationComponent testComponent;

  @Before
  public void registerMockServices() {
    testComponent = new CloudToolsPluginInitializationComponent();
    application = ApplicationManager.getApplication();
  }

  @After
  public void tearDown() {
    ApplicationManager.setApplication(application, disposable);
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
  public void testInitComponent_usageTrackigIsSet_whenAllConditionsAreMet() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(false);
    ApplicationManager.setApplication(mockApplication, disposable);

    when(usageTrackingManagementService.isUsageTrackingAvailable()).thenReturn(true);
    when(usageTrackingManagementService.hasUserRecordedTrackingPreference()).thenReturn(false);

    testComponent.initComponent();

    verify(usageTrackingManagementService).setTrackingPreference(true);
  }

  @Test
  public void testInitComponent_usageTrackigIsNotSet_whenTrackingNotAvailable() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(false);
    ApplicationManager.setApplication(mockApplication, disposable);

    when(usageTrackingManagementService.isUsageTrackingAvailable()).thenReturn(false);
    when(usageTrackingManagementService.hasUserRecordedTrackingPreference()).thenReturn(false);

    testComponent.initComponent();

    verify(usageTrackingManagementService, never()).setTrackingPreference(anyBoolean());
  }

  @Test
  public void testInitComponent_usageTrackigIsNotSet_whenTrackingPreferenceAlreadySet() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(false);
    ApplicationManager.setApplication(mockApplication, disposable);

    when(usageTrackingManagementService.isUsageTrackingAvailable()).thenReturn(true);
    when(usageTrackingManagementService.hasUserRecordedTrackingPreference()).thenReturn(true);

    testComponent.initComponent();

    verify(usageTrackingManagementService, never()).setTrackingPreference(anyBoolean());
  }

  @Test
  public void testInitComponent_usageTrackingIsEnabled() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(false);
    ApplicationManager.setApplication(mockApplication, disposable);
    testComponent = spy(new CloudToolsPluginInitializationComponent());
    doNothing().when(testComponent).configureUsageTracking();
    testComponent.initComponent();
    verify(testComponent).configureUsageTracking();
  }

  @Test
  public void testInitComponent_usageTrackingIsDisabled() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(true);
    ApplicationManager.setApplication(mockApplication, disposable);
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
