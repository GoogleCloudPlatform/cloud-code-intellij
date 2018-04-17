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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intellij.conversion.ModuleSettings;
import com.intellij.testFramework.PlatformTestCase;
import java.util.Collection;
import java.util.Collections;
import org.jdom.Element;

/** Unit tests for {@link AppEngineStandardFacetMigrationConversionProcessor} */
public class AppEngineStandardFacetMigrationConversionProcessorTest extends PlatformTestCase {

  private ModuleSettings moduleSettingsMock;
  private AppEngineStandardFacetMigrationConversionProcessor processor;
  private Collection testFacetElements;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // init mocks & test data
    moduleSettingsMock = mock(ModuleSettings.class);
    testFacetElements = Collections.singletonList(mock(Element.class));

    processor = new AppEngineStandardFacetMigrationConversionProcessor();
  }

  public void testIsConversionNeeded_oldFacetsAreNotPresent() {
    when(moduleSettingsMock.getFacetElements(getOldFacetId()))
        .thenReturn((Collection) Collections.emptyList());
    assertFalse(processor.isConversionNeeded(moduleSettingsMock));
  }

  public void testIsConversionNeeded_oldFacetsArePresent() {
    when(moduleSettingsMock.getFacetElements(getOldFacetId())).thenReturn(testFacetElements);
    assertTrue(processor.isConversionNeeded(moduleSettingsMock));
  }

  private String getOldFacetId() {
    return eq(AppEngineStandardFacetMigrationConversionProcessor.DEPRECATED_APP_ENGINE_FACET_ID);
  }
}
