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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.service.ApplicationPluginInfoService;
import com.google.cloud.tools.intellij.service.PluginConfigurationService;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.google.cloud.tools.intellij.testing.BasePluginTestCase;
import com.intellij.openapi.actionSystem.ActionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Tests to validate initialization on supported platforms */
@RunWith(MockitoJUnitRunner.class)
public class CloudToolsPluginInitializationComponentTest extends BasePluginTestCase {

  private static final String PLUGIN_ID_STRING = "com.google.gct.core";
  @Mock PluginInfoService pluginInfoService;
  @Mock PluginConfigurationService pluginConfigurationService;
  @Mock ActionManager actionManager;
  @Mock ApplicationPluginInfoService applicationInfoService;

  CloudToolsPluginInitializationComponent testComponent;

  @Before
  public void registerMockServices() {
    registerService(PluginInfoService.class, pluginInfoService);
    registerService(PluginConfigurationService.class, pluginConfigurationService);
    registerService(ActionManager.class, actionManager);
    registerService(ApplicationPluginInfoService.class, applicationInfoService);
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
}
