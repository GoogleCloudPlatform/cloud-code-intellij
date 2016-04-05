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

package com.google.cloud.tools.intellij;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineCloudType;
import com.google.cloud.tools.intellij.debugger.CloudDebugConfigType;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;

import com.intellij.openapi.extensions.ExtensionPointName;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests to validate initialization on supported platforms
 */

@RunWith(MockitoJUnitRunner.class)
public class CloudToolsPluginInitializationComponentTest extends BasePluginTestCase {

  private static final String PLUGIN_ID_STRING = "com.google.gct.core";
  @Mock
  CloudToolsPluginInfoService pluginInfoService;
  @Mock
  CloudToolsPluginConfigurationService pluginConfigurationService;
  CloudToolsPluginInitializationComponent testComponent;

  @Before
  public void registerMockServices() {
    registerService(CloudToolsPluginInfoService.class, pluginInfoService);
    registerService(CloudToolsPluginConfigurationService.class, pluginConfigurationService);
    testComponent = new CloudToolsPluginInitializationComponent();
  }

  @Test
  public void testInitComponent_debuggerIsEnabled() {
    when(pluginInfoService.shouldEnable(GctFeature.DEBUGGER)).thenReturn(true);
    testComponent.initComponent();
    verify(pluginConfigurationService).registerExtension(isA(ExtensionPointName.class),
        isA(CloudDebugConfigType.class));
  }


  @Test
  public void testInitComponent_debuggerIsDisabled() {
    when(pluginInfoService.shouldEnable(GctFeature.DEBUGGER)).thenReturn(false);
    testComponent.initComponent();
    verify(pluginConfigurationService, never())
        .registerExtension(isA(ExtensionPointName.class),
            isA(CloudDebugConfigType.class));
  }

  @Test
  public void testInitComponent_managedVmIsEnabled() {
    when(pluginInfoService.shouldEnable(GctFeature.APPENGINE_FLEX)).thenReturn(true);
    testComponent.initComponent();
    verify(pluginConfigurationService).registerExtension(isA(ExtensionPointName.class),
        isA(AppEngineCloudType.class));
  }


  @Test
  public void testInitComponent_managedVmIsDisabled() {
    when(pluginInfoService.shouldEnable(GctFeature.DEBUGGER)).thenReturn(false);
    testComponent.initComponent();
    verify(pluginConfigurationService, never())
        .registerExtension(isA(ExtensionPointName.class),
            isA(AppEngineCloudType.class));
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
}
