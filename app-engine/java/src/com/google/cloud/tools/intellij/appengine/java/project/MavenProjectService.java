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

package com.google.cloud.tools.intellij.appengine.java.project;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/** A set of helper methods for dealing with Maven-based projects * */
public class MavenProjectService {

  public static MavenProjectService getInstance() {
    return ServiceManager.getService(MavenProjectService.class);
  }

  /** Determines if the module is backed by maven. */
  public boolean isMavenModule(@NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(module.getProject());
    MavenProject mavenProject = projectsManager.findProject(module);

    return mavenProject != null && projectsManager.isMavenizedModule(module);
  }

  /** Determines if the module has jar or war packaging and is buildable by Maven. */
  public boolean isJarOrWarMavenBuild(@NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(module.getProject());
    MavenProject mavenProject = projectsManager.findProject(module);

    return mavenProject != null
        && isMavenModule(module)
        && ("jar".equalsIgnoreCase(mavenProject.getPackaging())
            || "war".equalsIgnoreCase(mavenProject.getPackaging()));
  }
}
