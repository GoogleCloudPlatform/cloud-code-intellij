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
import com.intellij.psi.xml.XmlFile;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public abstract boolean isFlexCompat(@Nullable XmlFile appEngineWebXml);

  public abstract boolean isFlexCompat(@NotNull Project project, @NotNull DeploymentSource source);

  public abstract boolean isFlexCompatEnvFlex(@NotNull Project project,
      @NotNull DeploymentSource source);

  /**
   * Determines the {@link AppEngineEnvironment} type of the module. This determination is made
   * based on the presence of an appengine-web.xml configuration file. If one exists, then it is
   * considered {@link AppEngineEnvironment#APP_ENGINE_STANDARD}.
   */
  @NotNull
  public abstract AppEngineEnvironment getModuleAppEngineEnvironment(
      @Nullable XmlFile appEngineWebXml);

  /**
   * {@code true} if the artifact type is an exploded-war.
   */
  public abstract boolean isAppEngineStandardArtifactType(@NotNull Artifact artifact);

  /**
   * {@code true} if the artifact type is a jar or war.
   */
  public abstract boolean isAppEngineFlexArtifactType(@NotNull Artifact artifact);


  /**
   * Determines if the module is backed by maven.
   */
  public abstract boolean isMavenModule(@NotNull Module module);

  /**
   * Determines if the module is backed by gradle.
   */
  public abstract boolean isGradleModule(@NotNull Module module);

  /**
   * Determines if the module has jar or war packaging and is buildable by Maven.
   */
  public abstract boolean isJarOrWarMavenBuild(@NotNull Project project, @NotNull Module module);
}
