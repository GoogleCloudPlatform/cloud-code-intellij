/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.PlatformTestCase;

/**
 * Tests for {@link AppEngineDeployToolsMenuAction}.
 */
public class AppEngineDeployToolsMenuActionTest extends PlatformTestCase {

  private AppEngineDeployToolsMenuAction action;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    action = new AppEngineDeployToolsMenuAction();
  }

  public void testIsAppEngineProjectCheck_noFacet() {
    assertFalse(action.isAppEngineProjectCheck(getProject()));
  }

  public void testIsAppEngineProjectCheck_flexibleFacet() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      FacetManager.getInstance(getModule()).addFacet(AppEngineFlexibleFacet.getFacetType(),
          "flex facet", null /* underlyingFacet */);
    });

    assertTrue(action.isAppEngineProjectCheck(getProject()));
  }

  public void testIsAppEngineProjectCheck_standardFacet() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      FacetManager.getInstance(getModule()).addFacet(AppEngineStandardFacet.getFacetType(),
          "standard facet", null /* underlyingFacet */);
    });

    assertTrue(action.isAppEngineProjectCheck(getProject()));
  }

}
