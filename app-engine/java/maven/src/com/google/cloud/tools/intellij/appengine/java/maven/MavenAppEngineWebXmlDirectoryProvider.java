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

import com.google.cloud.tools.intellij.appengine.java.facet.standard.BuildSystemAppEngineWebXmlDirectoryProvider;
import com.google.cloud.tools.intellij.appengine.java.maven.project.MavenProjectService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import java.util.Optional;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * Maven build-system aware strategy for providing the path to the appengine-web.xml directory.
 *
 * <p>Queries the maven-war-plugin for the warSourceDirectory attribute. If not present, then
 * returns the canonical default of 'src/main/webapp/WEB-INF'.
 */
public class MavenAppEngineWebXmlDirectoryProvider
    implements BuildSystemAppEngineWebXmlDirectoryProvider {

  private static final String MAVEN_WAR_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
  private static final String MAVEN_WAR_PLUGIN_ARTIFACT_ID = "maven-war-plugin";
  private static final String MAVEN_WAR_PLUGIN_WAR_DIR_ATTR_NAME = "warSourceDirectory";
  private static final String MAVEN_WAR_DEFAULT_DIR = "src/main/webapp";

  @Override
  public Optional<String> getAppEngineWebXmlDirectoryPath(@NotNull Module module) {
    if (!MavenProjectService.getInstance().isMavenModule(module)) {
      return Optional.empty();
    }

    MavenProject mavenProject =
        MavenProjectsManager.getInstance(module.getProject()).findProject(module);

    if (mavenProject == null) {
      return Optional.empty();
    }

    Element warPluginConfig =
        mavenProject.getPluginConfiguration(
            MAVEN_WAR_PLUGIN_GROUP_ID, MAVEN_WAR_PLUGIN_ARTIFACT_ID);

    String warSourceDirectory = null;

    if (warPluginConfig != null) {
      warSourceDirectory = warPluginConfig.getChildTextTrim(MAVEN_WAR_PLUGIN_WAR_DIR_ATTR_NAME);
    }

    if (warSourceDirectory == null) {
      warSourceDirectory = MAVEN_WAR_DEFAULT_DIR;
    }

    if (!FileUtil.isAbsolutePlatformIndependent(warSourceDirectory)) {
      warSourceDirectory = mavenProject.getDirectory() + '/' + warSourceDirectory;
    }

    return Optional.of(warSourceDirectory + "/WEB-INF");
  }
}
