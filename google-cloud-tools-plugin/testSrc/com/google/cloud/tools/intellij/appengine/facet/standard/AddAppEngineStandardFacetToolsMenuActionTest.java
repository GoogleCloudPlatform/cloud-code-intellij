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

package com.google.cloud.tools.intellij.appengine.facet.standard;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AddAppEngineStandardFacetToolsMenuAction}
 */
@RunWith(JUnit4.class)
public class AddAppEngineStandardFacetToolsMenuActionTest {

  @Test
  public void isActionNotRegistered() {
    assertNull(ActionManager.getInstance().getAction("AppAppEngineFacet.Standard"));
  }

  @Ignore
  // Temporarily disabled until implementation is complete
  public void isActionRegistered() {
    AnAction action = ActionManager.getInstance().getAction("AppAppEngineFacet.Standard");
    assertNotNull(action);
    Presentation presentation = action.getTemplatePresentation();
    assertEquals(GctBundle.message("appengine.standard.facet.name"), presentation.getText());
    assertEquals(GctBundle.message("appengine.add.standard.facet.tools.menu.description"),
        presentation.getDescription());
  }

}
