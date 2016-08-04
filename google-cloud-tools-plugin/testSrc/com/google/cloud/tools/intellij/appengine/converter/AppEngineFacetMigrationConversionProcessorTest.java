/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.converter;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.util.Plugins;

import com.intellij.conversion.ModuleSettings;
import com.intellij.testFramework.PlatformTestCase;

import org.jdom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Unit tests for {@link AppEngineFacetMigrationConversionProcessor}
 */
public class AppEngineFacetMigrationConversionProcessorTest extends PlatformTestCase {

  private Plugins pluginsMock;
  private ModuleSettings moduleSettingsMock;
  private AppEngineFacetMigrationConversionProcessor processor;
  private Collection testFacetElements;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // init mocks
    pluginsMock = mock(Plugins.class);
    moduleSettingsMock = mock(ModuleSettings.class);
    // init test data
    testFacetElements = Collections.singletonList(mock(Element.class));

    processor = new AppEngineFacetMigrationConversionProcessor(pluginsMock);
  }

  public void testIsConversionNeeded_oldPluginIsInstalled() {
    when(pluginsMock.isPluginInstalled(getOldPluginId())).thenReturn(true);
    when(moduleSettingsMock.getFacetElements(getOldFacetId())).thenReturn(testFacetElements);
    assertFalse(processor.isConversionNeeded(moduleSettingsMock));
  }

  public void testIsConversionNeeded_oldPluginIsNotInstalled_noOldFacetsPresent() {
    when(pluginsMock.isPluginInstalled(getOldPluginId())).thenReturn(false);
    when(moduleSettingsMock.getFacetElements(getOldFacetId())).thenReturn((Collection) Collections.emptyList());
    assertFalse(processor.isConversionNeeded(moduleSettingsMock));
  }
  public void testIsConversionNeeded_oldPluginIsNotInstalled_oldFacetsArePresent() {
    when(pluginsMock.isPluginInstalled(getOldPluginId())).thenReturn(false);
    when(moduleSettingsMock.getFacetElements(getOldFacetId())).thenReturn(testFacetElements);
    assertTrue(processor.isConversionNeeded(moduleSettingsMock));
  }

  private String getOldFacetId() {
    return eq(AppEngineFacetMigrationConversionProcessor.DEPRECATED_APP_ENGINE_FACET_ID);
  }

  private String getOldPluginId() {
    return eq(AppEngineFacetMigrationConversionProcessor.DEPRECATED_PLUGIN_ID);
  }
}

