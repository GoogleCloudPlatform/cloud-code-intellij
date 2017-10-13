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

package com.google.cloud.tools.intellij.appengine.facet.flexible;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import com.google.cloud.tools.intellij.appengine.facet.standard.AddAppEngineStandardFacetToolsMenuAction;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link AddAppEngineFlexibleFacetToolsMenuAction}
 */
@RunWith(JUnit4.class)
public class AddAppEngineFlexibleFacetToolsMenuActionTest {

  @Test
  public void isActionRegistered() {
    AnAction action = ActionManager.getInstance().getAction("AppAppEngineFacet.Flexible");
    assertNotNull(action);
    Presentation presentation = action.getTemplatePresentation();
    assertEquals(GctBundle.message("appengine.flexible.facet.name"), presentation.getText());
    assertEquals(GctBundle.message("appengine.add.flexible.facet.tools.menu.description"),
        presentation.getDescription());
  }

}
