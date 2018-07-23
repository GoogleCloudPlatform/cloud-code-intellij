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
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.Optional;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link GradleAppEngineWebXmlDirectoryProvider}. */
public class GradleAppEngineWebXmlDirectoryProviderTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  private @TestModule Module module;

  private GradleAppEngineWebXmlDirectoryProvider directoryProvider;

  private static final String MODULE_PATH = "/path/to/module";
  private static final String CUSTOM_WEBAPP_DIR = "custom/webapp-dir";
  private static final String ABSOLUTE_PATH_CUSTOM_WEBAPP_DIR =
      "/absolute/path/to/custom/webapp-dir";

  @Before
  public void setUp() {
    directoryProvider = new GradleAppEngineWebXmlDirectoryProvider();
  }

  @Test
  public void getPath_withNoGradleModule_returnsEmpty() {
    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getPath_withNoStoredWebAppDir_returnsCanonicalGradleDir() {
    setupGradleModule();

    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).isPresent()).isTrue();
    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).get())
        .isEqualTo(MODULE_PATH + "/src/main/webapp/WEB-INF");
  }

  @Test
  public void getPath_withStoredWebAppDir_returnsStoredWebAppDir() {
    setupGradleModule();
    AppEngineStandardGradleModuleComponent gradleModuleComponent =
        AppEngineStandardGradleModuleComponent.getInstance(module);
    gradleModuleComponent.setWebAppDir(CUSTOM_WEBAPP_DIR);

    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).isPresent()).isTrue();
    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).get())
        .isEqualTo(MODULE_PATH + "/" + CUSTOM_WEBAPP_DIR + "/WEB-INF");
  }

  @Test
  public void getPath_withStoredAbsoluteWebAppDir_returnsOnlyStoredWebAppDir() {
    setupGradleModule();
    AppEngineStandardGradleModuleComponent gradleModuleComponent =
        AppEngineStandardGradleModuleComponent.getInstance(module);
    gradleModuleComponent.setWebAppDir(ABSOLUTE_PATH_CUSTOM_WEBAPP_DIR);

    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).isPresent()).isTrue();
    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module).get())
        .isEqualTo(ABSOLUTE_PATH_CUSTOM_WEBAPP_DIR + "/WEB-INF");
  }

  private void setupGradleModule() {
    ExternalSystemModulePropertyManager.getInstance(module)
        .setExternalId(GradleConstants.SYSTEM_ID);

    AppEngineStandardGradleModuleComponent gradleModuleComponent =
        AppEngineStandardGradleModuleComponent.getInstance(module);
    gradleModuleComponent.setGradleModuleDir(MODULE_PATH);
  }
}
