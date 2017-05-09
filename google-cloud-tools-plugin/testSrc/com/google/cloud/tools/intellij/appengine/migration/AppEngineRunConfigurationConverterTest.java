package com.google.cloud.tools.intellij.appengine.migration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.RunManagerSettings;
import com.intellij.testFramework.PlatformTestCase;

import org.jdom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Unit tests for {@link AppEngineRunConfigurationConverter}.
 */
public class AppEngineRunConfigurationConverterTest extends PlatformTestCase {

  private AppEngineRunConfigurationConverter converter;
  private RunManagerSettings runManagerSettings;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    runManagerSettings = mock(RunManagerSettings.class);
    converter = new AppEngineRunConfigurationConverter();
  }

  public void testConversionIsNeeded_whenOldDeployRunConfigsPresent() {
    Element element = mock(Element.class);
    when(element.getAttributeValue("type")).thenReturn("google-app-engine-deploy");

    Collection legacyConfig = Collections.singletonList(element);
    when(runManagerSettings.getRunConfigurations()).thenReturn(legacyConfig);

    assertTrue(converter.isConversionNeeded(runManagerSettings));
  }

  public void testConversionIsNotNeeded_whenOldRunConfigsNotPresent() {
    when(runManagerSettings.getRunConfigurations()).thenReturn(Collections.emptyList());

    assertFalse(converter.isConversionNeeded(runManagerSettings));
  }

  public void testConversionIsNeeded_whenOldLocalRunConfigsPresent() {
    Element element = mock(Element.class);
    when(element.getAttributeValue("type")).thenReturn("GoogleAppEngineDevServer");

    Collection legacyConfig = Collections.singletonList(element);
    when(runManagerSettings.getRunConfigurations()).thenReturn(legacyConfig);

    assertTrue(converter.isConversionNeeded(runManagerSettings));
  }

  public void testProcessDeployConfig() throws CannotConvertException {
    Element element = mock(Element.class);
    when(element.getAttributeValue("type")).thenReturn("google-app-engine-deploy");

    Collection legacyConfig = Collections.singletonList(element);
    when(runManagerSettings.getRunConfigurations()).thenReturn(legacyConfig);

    converter.process(runManagerSettings);
    verify(element).setAttribute("type", "gcp-app-engine-deploy");
  }

  public void testProcessLocalRunConfig() throws CannotConvertException {
    Element element = mock(Element.class);
    when(element.getAttributeValue("type")).thenReturn("GoogleAppEngineDevServer");
    when(element.getName()).thenReturn("My Old Name");

    Collection legacyConfig = Collections.singletonList(element);
    when(runManagerSettings.getRunConfigurations()).thenReturn(legacyConfig);

    converter.process(runManagerSettings);
    verify(element).setName("My Old Name (migrated)");
  }
}
