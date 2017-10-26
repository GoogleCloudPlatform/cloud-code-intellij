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

package com.google.cloud.tools.intellij.appengine.facet;

import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.PlatformTestCase;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Provides tests for classes that inherit from {@link AddAppEngineFrameworkSupportAction}
 */
public abstract class AddAppEngineFrameworkSupportActionTest extends PlatformTestCase {

  public abstract @NotNull AddAppEngineFrameworkSupportAction getAction();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testGetSuitableModules_returnsAllModules_whenModulesHaveNoAppEngineFacet() {
    Project project = getProject();
    ModuleType moduleType = JavaModuleType.getModuleType();
    String path = project.getBaseDir().getPath();
    createModuleAt("module1", project, moduleType, path);
    createModuleAt("module2", project, moduleType, path);
    createModuleAt("module3", project, moduleType, path);

    List<Module> suitableModules = getAction().getSuitableModules(project);
    assertEquals(3, suitableModules.size());
  }

  public void testGetSuitableModules_returnsNonAppEngineModules_whenSomeModulesHaveAppEngineFacet() {
    Project project = getProject();
    ModuleType moduleType = JavaModuleType.getModuleType();
    String path = project.getBaseDir().getPath();
    createModuleAt("module1", project, moduleType, path);
    Module module2 = createModuleAt("module2", project, moduleType, path);
    Module module3 = createModuleAt("module3", project, moduleType, path);

    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        FacetType appEngineStandardFacet = AppEngineStandardFacet.getFacetType();
        FacetManager.getInstance(module2).
            addFacet(appEngineStandardFacet, appEngineStandardFacet.getPresentableName(), null);

        FacetType appEngineFlexibleFacet = AppEngineFlexibleFacet.getFacetType();
        FacetManager.getInstance(module3).
            addFacet(appEngineFlexibleFacet, appEngineFlexibleFacet.getPresentableName(), null);
      }
    }.execute();

    List<Module> suitableModules = getAction().getSuitableModules(project);
    assertEquals(1, suitableModules.size());
    assertEquals("module1", suitableModules.get(0).getName());
  }

}
