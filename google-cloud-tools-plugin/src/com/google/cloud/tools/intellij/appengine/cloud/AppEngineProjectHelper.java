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

package com.google.cloud.tools.intellij.appengine.cloud;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * A set of helper methods for inspecting an App Engine project's structure and configuration.
 */
public abstract class AppEngineProjectHelper {

  public static final String APP_ENGINE_STANDARD_FACET_NAME = "Google App Engine";

  public static AppEngineProjectHelper getInstance() {
    return ServiceManager.getService(AppEngineProjectHelper.class);
  }

  /**
   * Given a deployment source, returns the xml tag corresponding to the project's appengine-web.xml
   * compat configuration or null if there isn't one.
   */
  @Nullable
  public abstract XmlTag getFlexCompatXmlConfiguration(@NotNull Project project,
      @NotNull DeploymentSource source);


  /**
   * @see AppEngineProjectHelper#getFlexCompatXmlConfiguration(Project, DeploymentSource).
   */
  @Nullable
  public abstract XmlTag getFlexCompatXmlConfiguration(@NotNull Project project,
      @Nullable Artifact artifact);


  /**
   * Determines if a project is set up like an App Engine standard project but is configured in
   * 'compatibility' mode. This indicates that the project runs in the flexible environment.
   *
   * <p>A flex compat project has an appengine-web.xml with either:
   * {
   * @code <vm>true</vm>
   * <env>flex</env>
   * }
   */
  public abstract boolean isFlexCompat(@NotNull Project project, @NotNull DeploymentSource source);

  /**
   * @see AppEngineProjectHelper#isFlexCompat(Project, DeploymentSource).
   */
  public abstract boolean isFlexCompat(@NotNull Project project, @Nullable Artifact artifact);

  /**
   * Determines the {@link AppEngineEnvironment} type of the artifact.
   */
  @Nullable
  public abstract AppEngineEnvironment getAppEngineArtifactEnvironment(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * Returns the project's appengine-web.xml or null if it does not exist.
   */
  @Nullable
  public abstract XmlFile loadAppEngineStandardWebXml(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * Determines if a module has App Engine standard deployable artifacts. An artifact is considered
   * deployable to App Engine standard if it has the AE facet and it is an exploded war artifact
   * type.
   */
  public abstract boolean containsAppEngineStandardArtifacts(@NotNull Project project,
      @NotNull Module module);

  /**
   * @see AppEngineProjectHelper#containsAppEngineStandardArtifacts(Project, Module).
   */
  public abstract boolean containsAppEngineStandardArtifacts(Project project,
      Collection<Artifact> artifacts);


  public abstract boolean isAppEngineStandardArtifact(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * True if the artifact has the App Engine standard facet.
   */
  public abstract boolean hasAppEngineStandardFacet(@NotNull Project project,
      @NotNull Artifact artifact);

  /**
   * True if the artifact type is an exploded-war.
   */
  public abstract boolean isAppEngineStandardArtifactType(@NotNull Artifact artifact);

  /**
   * True if the artifact type is a jar or war.
   */
  public abstract boolean isAppEngineFlexArtifactType(@NotNull Artifact artifact);

  /**
   * Determines if the module has jar or war packaging and is buildable by Maven.
   */
  public abstract boolean isJarOrWarMavenBuild(@NotNull Project project, @NotNull Module module);
}
