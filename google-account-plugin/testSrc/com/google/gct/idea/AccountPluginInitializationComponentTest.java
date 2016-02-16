package com.google.gct.idea;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gct.idea.testing.BasePluginTestCase;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests to validate initialization on supported platforms
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountPluginInitializationComponentTest extends BasePluginTestCase {

  private static final String PLUGIN_ID_STRING = "com.google.gct.login";
  @Mock
  AccountPluginInfoService pluginInfoService;
  @Mock
  AccountPluginConfigurationService pluginConfigurationService;
  AccountPluginInitializationComponent testComponent;


  @Before
  public void registerMockServices() {
    registerService(AccountPluginInfoService.class, pluginInfoService);
    registerService(AccountPluginConfigurationService.class, pluginConfigurationService);
    testComponent = new AccountPluginInitializationComponent();
  }

  // TODO: factor out commonality in tests and application components for feedback initialization
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
    AccountPluginInitializationComponent testComponent = spy(new AccountPluginInitializationComponent());
    doNothing().when(testComponent).configureUsageTracking();
    testComponent.initComponent();
    verify(testComponent).configureUsageTracking();
  }

  @Test
  public void testInitComponent_usageTrackingIsDisabled() {
    Application mockApplication = spy(ApplicationManager.getApplication());
    when(mockApplication.isUnitTestMode()).thenReturn(true);
    ApplicationManager.setApplication(mockApplication, mock(Disposable.class));
    AccountPluginInitializationComponent testComponent = spy(new AccountPluginInitializationComponent());
    testComponent.initComponent();
    verify(testComponent, never()).configureUsageTracking();
  }
}
