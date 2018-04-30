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

package com.google.cloud.tools.intellij.appengine.java.migration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.conversion.RunManagerSettings;
import com.intellij.testFramework.PlatformTestCase;
import java.util.Collection;
import java.util.Collections;
import org.jdom.Element;

/** Unit tests for {@link AppEngineDeploymentRunConfigurationConverter}. */
public class AppEngineDeploymentRunConfigurationConverterTest extends PlatformTestCase {

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
