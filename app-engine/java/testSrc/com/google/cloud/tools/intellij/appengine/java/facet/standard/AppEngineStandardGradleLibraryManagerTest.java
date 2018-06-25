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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineStandardGradleLibraryManager}. */
public class AppEngineStandardGradleLibraryManagerTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  private AppEngineStandardGradleLibraryManager gradleLibraryManager;

  @Before
  public void setUp() {
    gradleLibraryManager = new AppEngineStandardGradleLibraryManager();
  }

  @Test
  public void isSupported_withGradle_isFalse() {
    ExternalSystemModulePropertyManager.getInstance(module)
        .setExternalId(GradleConstants.SYSTEM_ID);
    assertThat(gradleLibraryManager.isSupported(module)).isFalse();
  }

  @Test
  public void isSupported_withoutGradle_isTrue() {
    assertThat(gradleLibraryManager.isSupported(module)).isTrue();
  }
}
