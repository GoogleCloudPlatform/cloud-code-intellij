package com.google.cloud.tools.intellij.appengine.migration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.conversion.RunManagerSettings;
import com.intellij.testFramework.PlatformTestCase;

import org.jdom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Unit tests for {@link AppEngineDeploymentRunConfigurationConverter}.
 */
public class AppEngineDeploymentRunConfigurationConverterTest extends PlatformTestCase{

  private AppEngineDeploymentRunConfigurationConverter converter;
  private RunManagerSettings runManagerSettings;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    runManagerSettings = mock(RunManagerSettings.class);
    converter = new AppEngineDeploymentRunConfigurationConverter();
  }

  public void testConversionIsNeeded_whenOldRunConfigurationsPresent() {
    Element element = mock(Element.class);
    when(element.getAttributeValue("type")).thenReturn("google-app-engine-deploy");

    Collection legacyConfig = Collections.singletonList(element);
    when(runManagerSettings.getRunConfigurations()).thenReturn(legacyConfig);

    assertTrue(converter.isConversionNeeded(runManagerSettings));
  }

  public void testConversionIsNotNeeded_whenOldRunConfigurationsNotPresent() {
    when(runManagerSettings.getRunConfigurations()).thenReturn(Collections.emptyList());

    assertFalse(converter.isConversionNeeded(runManagerSettings));
  }

}
