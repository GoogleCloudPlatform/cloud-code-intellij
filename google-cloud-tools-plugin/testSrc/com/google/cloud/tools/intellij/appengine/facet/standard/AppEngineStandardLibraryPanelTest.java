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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import javax.swing.JCheckBox;

/**
 * Tests for {@link AppEngineStandardLibraryPanel}.
 */
public class AppEngineStandardLibraryPanelTest {

  private AppEngineStandardLibraryPanel panel;

  @Before
  public void setUp() {
    panel = new AppEngineStandardLibraryPanel(true /*enabled*/);
  }

  @Test
  public void testServletApiSelectedByDefault() {
    for (JCheckBox library : panel.getLibraries()) {
      if (AppEngineStandardMavenLibrary.SERVLET_API.getDisplayName().equals(library.getText())) {
        assertTrue(library.isSelected());
      } else {
        assertFalse(library.isSelected());
      }
    }
  }


  @Test
  public void testObjectifySelection_selectsAppEngineApi() {
    JCheckBox appEngineApiCheckbox
        = panel.getLibraryCheckbox(AppEngineStandardMavenLibrary.APP_ENGINE_API.getDisplayName());

    // App Engine API checkbox is initially unchecked
    assertFalse(appEngineApiCheckbox.isSelected());

    // Select Objectify
    panel.selectLibraryByName(AppEngineStandardMavenLibrary.OBJECTIFY.getDisplayName());

    // App Engine API should now be selected
    assertTrue(appEngineApiCheckbox.isSelected());
  }

  @Test
  public void testEndpointsSelection_selectsAppEngineApi() {
    JCheckBox appEngineApiCheckbox
        = panel.getLibraryCheckbox(AppEngineStandardMavenLibrary.APP_ENGINE_API.getDisplayName());

    // App Engine API checkbox is initially unchecked
    assertFalse(appEngineApiCheckbox.isSelected());

    // Select Endpoints
    panel.selectLibraryByName(AppEngineStandardMavenLibrary.ENDPOINTS.getDisplayName());

    // App Engine API should now be selected
    assertTrue(appEngineApiCheckbox.isSelected());
  }

  @Test
  public void testUnselectAppEngineApi_unselectsDependencies() {
    // Select both Objectify and Endpoints
    JCheckBox objectifyCheckbox
        = panel.getLibraryCheckbox(AppEngineStandardMavenLibrary.OBJECTIFY.getDisplayName());
    JCheckBox endpointsCheckbox
        = panel.getLibraryCheckbox(AppEngineStandardMavenLibrary.ENDPOINTS.getDisplayName());
    objectifyCheckbox.setSelected(true);
    endpointsCheckbox.setSelected(true);

    // Uncheck the App Engine API
    JCheckBox appEngineApiCheckbox
        = panel.getLibraryCheckbox(AppEngineStandardMavenLibrary.APP_ENGINE_API.getDisplayName());
    appEngineApiCheckbox.setSelected(false);

    assertFalse(objectifyCheckbox.isSelected());
    assertFalse(endpointsCheckbox.isSelected());
  }
}
