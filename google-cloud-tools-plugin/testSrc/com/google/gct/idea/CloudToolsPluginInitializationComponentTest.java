package com.google.gct.idea;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gct.idea.debugger.CloudDebugConfigType;
import com.google.gct.idea.testing.BasePluginTestCase;

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
