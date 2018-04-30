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

package com.google.cloud.tools.intellij.appengine.java.facet.standard;

import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.google.cloud.tools.intellij.appengine.java.facet.AddAppEngineFrameworkSupportAction;
import com.google.cloud.tools.intellij.appengine.java.facet.AddAppEngineFrameworkSupportActionTest;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

/** Tests for {@link AddAppEngineStandardFrameworkSupportToolsMenuAction} */
public class AddAppEngineStandardFrameworkSupportToolsMenuActionTest
    extends AddAppEngineFrameworkSupportActionTest {
  private AddAppEngineStandardFrameworkSupportToolsMenuAction action;

  @NotNull
  @Override
  public AddAppEngineFrameworkSupportAction getAction() {
    return action;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    action = new AddAppEngineStandardFrameworkSupportToolsMenuAction();
  }

  public void testIsActionRegistered() {
    AnAction action =
        ActionManager.getInstance().getAction("AddAppEngineFrameworkSupport.Standard");
    assertNotNull(action);
    Presentation presentation = action.getTemplatePresentation();
    assertEquals(
        AppEngineMessageBundle.message("appengine.standard.facet.name.title"),
        presentation.getText());
    assertEquals(
        AppEngineMessageBundle.message(
            "appengine.add.framework.support.tools.menu.description",
            AppEngineMessageBundle.message("appengine.standard.facet.name")),
        presentation.getDescription());
  }

  @Override
  public void testGetSuitableModules_returnsAllModules_whenModulesHaveNoAppEngineFacet() {
    super.testGetSuitableModules_returnsAllModules_whenModulesHaveNoAppEngineFacet();
  }

  @Override
  public void
      testGetSuitableModules_returnsNonAppEngineModules_whenSomeModulesHaveAppEngineFacet() {
    super.testGetSuitableModules_returnsNonAppEngineModules_whenSomeModulesHaveAppEngineFacet();
  }
}
