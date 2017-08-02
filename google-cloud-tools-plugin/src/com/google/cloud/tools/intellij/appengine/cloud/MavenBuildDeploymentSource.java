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

import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.impl.configuration.deployment.ModuleDeploymentSourceImpl;
import icons.MavenIcons;
import java.io.File;
import java.util.Optional;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * A deployment source backed by the Maven build system.
 */
public class MavenBuildDeploymentSource extends ModuleDeploymentSourceImpl
    implements AppEngineDeployable {

  private static final Logger logger = Logger.getInstance(MavenBuildDeploymentSource.class);

  private final Project project;
  private AppEngineEnvironment environment;
  private String name;
  private String projectName;
  private String version;

  /**
   * Default constructor used instantiating plain Maven Build Deployment sources.
   */
  public MavenBuildDeploymentSource(@NotNull ModulePointer pointer, @NotNull Project project) {
    super(pointer);
    this.project = project;
    this.name = getDefaultName();
  }

  public MavenBuildDeploymentSource(@NotNull ModulePointer pointer,
      @NotNull Project project,
      @NotNull AppEngineEnvironment environment) {
    super(pointer);
    this.project = project;
    this.environment = environment;
    this.name = getDefaultName();
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return name;
  }

  @Override
  public String getProjectName() {
    return projectName;
  }

  @Override
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  private String getDefaultName() {
    return "Maven build: " + getModulePointer().getModuleName();
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

    final StringBuilder targetBuild =
        new StringBuilder(
            new File(mavenProject.getBuildDirectory()).getPath()
                + File.separator
                + mavenProject.getFinalName());

    AppEngineProjectService projectService = AppEngineProjectService.getInstance();

    // The environment will be null for newly deserialized deployment sources to ensure freshness.
    // In this case, we need to try and reload the environment.
    if (environment == null) {
      Optional<AppEngineEnvironment> appEngineEnvironment =
          projectService.getModuleAppEngineEnvironment(getModule());

      appEngineEnvironment.ifPresent(
          env -> {
            environment = env;
            if (env.isFlexible()) {
              targetBuild.append(".");
              targetBuild.append(mavenProject.getPackaging());
            }
          });
    }

    return new File(targetBuild.toString());
  }

  @Override
  public AppEngineEnvironment getEnvironment() {
    return environment;
  }
}
