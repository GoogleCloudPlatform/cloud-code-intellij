/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.appengine.project;

import com.google.cloud.tools.intellij.appengine.cloud.AppEngineEnvironment;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A set of helper methods for inspecting an App Engine project's structure and configuration.
 */
public abstract class AppEngineProjectService {

  public static final String APP_ENGINE_STANDARD_FACET_NAME = "Google App Engine";

  public static AppEngineProjectService getInstance() {
    return ServiceManager.getService(AppEngineProjectService.class);
  }

  /**
   * Determines if a deployment source is set up like an App Engine standard deployable but is
   * configured in 'compatibility' mode. This indicates that the deployable runs in the flexible
   * environment.
   *
   * <p>A flex compat deployment source has an appengine-web.xml with either:
   * {@code
   * <vm>true</vm>
   * <env>flex</env>
   * }
   */
  public abstract boolean isFlexCompat(@NotNull Project project, @NotNull DeploymentSource source);

  /**
   * @see AppEngineProjectService#isFlexCompat(Project, DeploymentSource).
   */
  public abstract boolean isFlexCompat(@NotNull Project project, @NotNull Artifact artifact);

  public abstract boolean isFlexCompatEnvFlex(@NotNull Project project,
      @NotNull DeploymentSource source);

  /**
   * Determines the {@link AppEngineEnvironment} type of the artifact.
   */
  @Nullable
  public abstract AppEngineEnvironment getAppEngineArtifactEnvironment(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * Determines if a module has App Engine standard deployable artifacts. An artifact is considered
   * deployable to App Engine standard if it has the AE facet and it is an exploded war artifact
   * type.
   */
  public abstract boolean containsAppEngineStandardArtifacts(@NotNull Project project,
      @NotNull Module module);

  /**
   * @see AppEngineProjectService#containsAppEngineStandardArtifacts(Project, Module).
   */
  public abstract boolean containsAppEngineStandardArtifacts(Project project,
      Collection<Artifact> artifacts);

  /**
   * @see AppEngineProjectService#containsAppEngineStandardArtifacts(Project, Module).
   */
  public abstract boolean isAppEngineStandardArtifact(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * {@code true} if the artifact is associated with a module that has the App Engine standard
   * facet.
   */
  public abstract boolean hasAppEngineStandardFacet(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * {@code true} if the artifact type is an exploded-war.
   */
  public abstract boolean isAppEngineStandardArtifactType(@NotNull Artifact artifact);

  /**
   * {@code true} if the artifact type is a jar or war.
   */
  public abstract boolean isAppEngineFlexArtifactType(@NotNull Artifact artifact);

  /**
   * Determines if the module has jar or war packaging and is buildable by Maven.
   */
  public abstract boolean isJarOrWarMavenBuild(@NotNull Project project, @NotNull Module module);
}
