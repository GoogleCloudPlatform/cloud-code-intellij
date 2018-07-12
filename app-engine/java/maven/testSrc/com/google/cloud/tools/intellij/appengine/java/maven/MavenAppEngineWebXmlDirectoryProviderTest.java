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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.intellij.testing.CloudToolsRule;
import com.google.cloud.tools.intellij.testing.MavenTestUtils;
import com.google.cloud.tools.intellij.testing.TestFixture;
import com.google.cloud.tools.intellij.testing.TestModule;
import com.intellij.openapi.components.impl.ComponentManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import java.util.Optional;
import org.jdom.Element;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

/** Tests for {@link MavenAppEngineWebXmlDirectoryProvider}. */
public class MavenAppEngineWebXmlDirectoryProviderTest {

  @Rule public final CloudToolsRule cloudToolsRule = new CloudToolsRule(this);
  @TestFixture private IdeaProjectTestFixture testFixture;

  private @TestModule Module module;

  @Mock private MavenProjectsManager mavenProjectsManager;
  @Mock private MavenProject mavenProject;
  @Mock private Element warPluginDom;

  private MavenAppEngineWebXmlDirectoryProvider directoryProvider;

  @Before
  public void setUp() {
    directoryProvider = new MavenAppEngineWebXmlDirectoryProvider();
  }

  @Test
  public void getPath_withNoMavenProject_returnsEmpty() {
    assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(module))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getPath_withMavenProject_andNoWarSourceDirectory_returnsCanonicalMavenDir() {
    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            mavenModule -> {
              assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(mavenModule).isPresent())
                  .isTrue();
              assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(mavenModule).get())
                  .endsWith("/src/main/webapp/WEB-INF");
            });
  }

  @Test
  public void
      getPath_withMavenProject_andRelativeWarSourceDirectory_returnsWarSourceDir_withBasePrefix() {
    String customWebDirPath = "path/to/web/dir";
    String baseModulePath = "/path/to/module";

    ((ComponentManagerImpl) module)
        .registerComponentInstance(MavenProjectsManager.class, mavenProjectsManager);
    when(mavenProjectsManager.findProject(any(Module.class))).thenReturn(mavenProject);
    when(mavenProjectsManager.isMavenizedModule(any(Module.class))).thenReturn(true);
    when(mavenProject.getPluginConfiguration(anyString(), anyString())).thenReturn(warPluginDom);
    when(mavenProject.getDirectory()).thenReturn(baseModulePath);
    when(warPluginDom.getChildTextTrim("warSourceDirectory")).thenReturn(customWebDirPath);

    MavenTestUtils.getInstance()
        .runWithMavenModule(
            testFixture.getProject(),
            mavenModule -> {
              assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(mavenModule).isPresent())
                  .isTrue();
              assertThat(directoryProvider.getAppEngineWebXmlDirectoryPath(mavenModule).get())
                  .isEqualTo(baseModulePath + "/" + customWebDirPath + "/WEB-INF");
            });
  }
}
