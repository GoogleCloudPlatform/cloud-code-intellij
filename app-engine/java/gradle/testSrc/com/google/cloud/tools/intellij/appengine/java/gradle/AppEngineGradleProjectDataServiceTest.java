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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.appengine.java.facet.standard.AppEngineStandardGradleModuleComponent;
import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.google.cloud.tools.intellij.testing.TestScopedSystemPropertyRule;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.util.PlatformUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineGradleProjectDataService}. */
public class AppEngineGradleProjectDataServiceTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @Rule
  public final TestScopedSystemPropertyRule systemPropertyRule =
      new TestScopedSystemPropertyRule(PlatformUtils.PLATFORM_PREFIX_KEY);

  private AppEngineGradleProjectDataService dataService;
  private IdeModifiableModelsProvider modelsProvider;
  private String originalPlatformPrefix;
  private AppEngineStandardGradleModuleComponent gradleModuleComponent;

  private static final String GRADLE_BUILD_DIR = "/path/to/build/dir";
  private static final String GRADLE_MODULE_DIR = "/path/to/module";

  @TestModule private Module module;

  @Before
  public void setUp() {
    dataService = new AppEngineGradleProjectDataService();
    modelsProvider = new IdeModifiableModelsProviderImpl(testFixture.getProject());
    gradleModuleComponent = AppEngineStandardGradleModuleComponent.getInstance(module);
  }

  @Test
  public void importData_withAppEngineGradleModel_andGradlePlugin_savesGradleData() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);

    createModelAndImportData(true /*hasPlugin*/);

    assertThat(gradleModuleComponent.getGradleBuildDir().isPresent()).isTrue();
    assertThat(gradleModuleComponent.getGradleModuleDir().isPresent()).isTrue();
  }

  @Test
  public void importData_withAppEngineGradleModel_andNoGradlePlugin_doesNotSaveGradleData() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_CE_PREFIX);

    createModelAndImportData(false /*hasPlugin*/);

    assertThat(gradleModuleComponent.getGradleBuildDir().isPresent()).isFalse();
    assertThat(gradleModuleComponent.getGradleModuleDir().isPresent()).isFalse();
  }

  @Test
  public void importData_withGradlePlugin_andIdeaUltimateEdition_doesNotSaveGradleData() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, PlatformUtils.IDEA_PREFIX);

    createModelAndImportData(true /*hasPlugin*/);

    assertThat(gradleModuleComponent.getGradleBuildDir().isPresent()).isFalse();
    assertThat(gradleModuleComponent.getGradleModuleDir().isPresent()).isFalse();
  }

  private void createModelAndImportData(boolean hasAppEngineGradlePlugin) {
    AppEngineGradleModel model =
        new DefaultAppEngineGradleModel(
            hasAppEngineGradlePlugin, GRADLE_BUILD_DIR, GRADLE_MODULE_DIR);
    AppEngineGradleModule appEngineGradleModule =
        new AppEngineGradleModule(module.getName(), model);

    DataNode<AppEngineGradleModule> dataNode =
        new DataNode<>(
            AppEngineGradleProjectDataService.APP_ENGINE_MODEL_KEY,
            appEngineGradleModule,
            null /*parent*/);

    dataService.importData(
        ImmutableList.of(dataNode), null /*projectData*/, testFixture.getProject(), modelsProvider);
  }
}
