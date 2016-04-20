/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;

import javax.swing.Icon;

import icons.MavenIcons;

/**
 * A deployment source backed by the Maven build system
 */
public class MavenBuildDeploymentSource extends ModuleDeploymentSourceImpl {

  private final Project project;

  public MavenBuildDeploymentSource(@NotNull ModulePointer pointer, @NotNull Project project) {
    super(pointer);
    this.project = project;
  }

  @NotNull
  @Override
  public String getPresentableName() {
     return String.format("Maven build: %s", getModulePointer().getModuleName());
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return MavenIcons.MavenLogo;
  }

  @NotNull
  @Override
  public DeploymentSourceType<?> getType() {
    return DeploymentSourceType.EP_NAME.findExtension(MavenBuildDeploymentSourceType.class);
  }

  @Nullable
  @Override
  public File getFile() {
    if (getModule() == null) {
      return null;
    }

    MavenProject mavenProject =
        MavenProjectsManager.getInstance(project).findProject(getModule());

    if (mavenProject == null) {
      return null;
    }

    String targetBuild =
        new File(mavenProject.getBuildDirectory()).getPath() + File.separator +
        mavenProject.getFinalName() + "." +
        mavenProject.getPackaging();

    return new File(targetBuild);
  }
}
