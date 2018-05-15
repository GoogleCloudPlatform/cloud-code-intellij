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

package com.google.cloud.tools.intellij.appengine.java.gradle;

import static org.mockito.Mockito.verify;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

public class AppEngineGradleProjectDataServiceTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  private AppEngineGradleProjectDataService dataService;
  private IdeModifiableModelsProvider modelsProvider;

  @TestModule private Module module;
  @Mock private AppEngineGradleFacetService facetService;

  @Before
  public void setUp() {
    dataService = new AppEngineGradleProjectDataService(facetService);
    modelsProvider = new IdeModifiableModelsProviderImpl(testFixture.getProject());
  }

  @Test
  public void importData_withAppEngineGradleModel_andGradlePlugin_addsFacet() {
    AppEngineGradleModel model = new DefaultAppEngineGradleModel(true /*hasPlugin*/, "test/path");
    AppEngineGradleModule appEngineGradleModule =
        new AppEngineGradleModule(module.getName(), model);

    DataNode<AppEngineGradleModule> dataNode =
        new DataNode<>(
            AppEngineGradleProjectDataService.APP_ENGINE_MODEL_KEY,
            appEngineGradleModule,
            null /*parent*/);

    dataService.importData(
        ImmutableList.of(dataNode), null /*projectData*/, testFixture.getProject(), modelsProvider);

    verify(facetService)
        .addFacet(appEngineGradleModule, module, modelsProvider.getModifiableFacetModel(module));
  }
}
