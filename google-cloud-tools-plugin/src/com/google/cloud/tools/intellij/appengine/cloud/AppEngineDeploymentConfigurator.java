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

import com.google.cloud.tools.intellij.appengine.cloud.CloudSdkAppEngineHelper.Environment;
import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;
import com.google.common.base.Predicate;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointer;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.JavaDeploymentSourceUtil;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sets up the configuration elements for an AppEngine Cloud deployment.
 */
class AppEngineDeploymentConfigurator extends
    DeploymentConfigurator<AppEngineDeploymentConfiguration, AppEngineServerConfiguration> {

  private final Project project;
  private Environment environment;

  public AppEngineDeploymentConfigurator(Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public List<DeploymentSource> getAvailableDeploymentSources() {
    List<DeploymentSource> deploymentSources = new ArrayList<>();

    deploymentSources.addAll(JavaDeploymentSourceUtil
        .getInstance().createArtifactDeploymentSources(project, collectArtifacts()));

    deploymentSources.addAll(collectModules());

    return deploymentSources;
  }

  private List<Artifact> collectArtifacts() {
    List<Artifact> artifacts = new ArrayList<>();

    if (getEnvironment() == Environment.APP_ENGINE_STANDARD) {
      artifacts.addAll(collectArtifacts(new Predicate<Artifact>() {
        @Override
        public boolean apply(Artifact artifact) {
          return AppEngineUtil.isAppEngineStandardArtifactType(artifact);
        }
      }));
    } else {
      artifacts.addAll(collectArtifacts(new Predicate<Artifact>() {
        @Override
        public boolean apply(Artifact artifact) {
          return AppEngineUtil.isAppEngineFlexArtifactType(artifact);
        }
      }));
    }

    return artifacts;
  }

  private List<Artifact> collectArtifacts(Predicate<Artifact> shouldCollect) {
    List<Artifact> artifacts = new ArrayList<>();

    for (Artifact artifact : ArtifactManager.getInstance(project).getArtifacts()) {
      if (shouldCollect.apply(artifact)) {
        artifacts.add(artifact);
      }
    }

    Collections.sort(artifacts, ArtifactManager.ARTIFACT_COMPARATOR);
    return artifacts;
  }

  private List<ModuleDeploymentSource> collectModules() {
    if (getEnvironment() == Environment.APP_ENGINE_STANDARD) {
      return Collections.emptyList();
    }

    List<ModuleDeploymentSource> moduleDeploymentSources = new ArrayList<>();

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (isJarOrWarMavenBuild(module)) {
        moduleDeploymentSources.add(
            new MavenBuildDeploymentSource(
                ModulePointerManager.getInstance(project).create(module), project));
      }
    }

    ModulePointer modulePointer =
        ModulePointerManager.getInstance(project).create("userSpecifiedSource");
    moduleDeploymentSources.add(new UserSpecifiedPathDeploymentSource(modulePointer));

    return moduleDeploymentSources;
  }

  private boolean isJarOrWarMavenBuild(Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
    MavenProject mavenProject = projectsManager.findProject(module);

    boolean isMavenProject = projectsManager.isMavenizedModule(module)
        && mavenProject != null;

    return isMavenProject
        && ("jar".equalsIgnoreCase(mavenProject.getPackaging())
        || "war".equalsIgnoreCase(mavenProject.getPackaging()));

  }

  @NotNull
  @Override
  public AppEngineDeploymentConfiguration createDefaultConfiguration(
      @NotNull DeploymentSource source) {
    return new AppEngineDeploymentConfiguration();
  }

  @Nullable
  @Override
  public SettingsEditor<AppEngineDeploymentConfiguration> createEditor(
      @NotNull DeploymentSource source,
      @NotNull RemoteServer<AppEngineServerConfiguration> server) {
    return new AppEngineDeploymentRunConfigurationEditor(
        project,
        source,
        new CloudSdkAppEngineHelper(
            project,
            new File(server.getConfiguration().getCloudSdkHomePath()),
            getEnvironment())
    );
  }

  /**
   * Lazily initialize the environment because this class is instantiated on IDE boot time
   * and the project may not have yet been fully constructed.
   */
  public Environment getEnvironment() {
    if (environment == null) {
      environment = AppEngineUtil.isAppEngineStandardProject(project)
          ? Environment.APP_ENGINE_STANDARD
          : Environment.APP_ENGINE_FLEX;
    }

    return environment;
  }
}
