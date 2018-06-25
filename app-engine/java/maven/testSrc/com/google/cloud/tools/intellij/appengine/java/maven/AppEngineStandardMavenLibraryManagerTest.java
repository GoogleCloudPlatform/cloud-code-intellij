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

package com.google.cloud.tools.intellij.appengine.java.maven;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.MavenTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Tests for {@link AppEngineStandardMavenLibraryManager}. */
public class AppEngineStandardMavenLibraryManagerTest {
  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  @TestModule private Module module;

  private AppEngineStandardMavenLibraryManager mavenLibraryManager;

  @Before
  public void setUp() {
    mavenLibraryManager = new AppEngineStandardMavenLibraryManager();
  }

  @Test
  public void isSupported_withMaven_isFalse() {
    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            mavenModule -> assertThat(mavenLibraryManager.isSupported(mavenModule)).isFalse());
  }

  @Test
  public void isSupported_withoutMaven_isTrue() {
    assertThat(mavenLibraryManager.isSupported(module)).isTrue();
  }
}
