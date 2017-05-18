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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleSupportProvider.AppEngineFlexibleSupportConfigurable;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.PlatformTestCase;

import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link AppEngineFlexibleSupportConfigurable}.
 */
public class AppEngineFlexibleSupportConfigurableTest extends PlatformTestCase {

  private static AppEngineFlexibleSupportConfigurable supportConfigurable;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    supportConfigurable = spy(new AppEngineFlexibleSupportConfigurable());
  }

  public void testAddSupportPostStartup() {
    Module module = createModule("testModule");

    doNothing().when(supportConfigurable)
        .addAppEngineFlexibleSupport(
            any(ModifiableRootModel.class), any(AppEngineFlexibleFacet.class));

    new WriteAction() {
      @Override
      protected void run(@NotNull Result result) throws Throwable {
        supportConfigurable
            .addSupport(module, mock(ModifiableRootModel.class), mock(ModifiableModelsProvider.class));
      }
    }.execute();

    assertNotNull(FacetManager.getInstance(module).getFacetByType(AppEngineFlexibleFacetType.ID));
  }
}
