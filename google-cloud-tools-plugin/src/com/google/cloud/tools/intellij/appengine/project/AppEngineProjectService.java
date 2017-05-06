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
import com.google.cloud.tools.intellij.appengine.cloud.flexible.AppEngineFlexibleDeploymentArtifactType;
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardRuntime;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.xml.XmlFile;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A set of helper methods for inspecting an App Engine project's structure and configuration.
 */
public abstract class AppEngineProjectService {

  /**
   * Represents the target App Engine runtime as defined in the app.yaml configuration file via
   * the 'runtime: [custom|java]' field.
   */
  public enum FlexibleRuntime {
    CUSTOM,
    JAVA;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

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

  /**
   * Determines the {@link AppEngineEnvironment} type of the module. If {@code module} contains an
   * {@link com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet}, it is
   * considered Flexible. If it contains an
   * {@link com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet}, it is
   * considered Standard if its appengine-web.xml doesn't contain <vm>true</vm> or <env>flex</env>.
   *
   */
  public abstract Optional<AppEngineEnvironment> getModuleAppEngineEnvironment(Module module);

  /**
   * Returns the declared {@link AppEngineStandardRuntime} in appengine-web.xml. If there is no
   * appengine-web.xml, or if no runtime is declared, returns null.
   */
  @Nullable
  public abstract AppEngineStandardRuntime getAppEngineStandardDeclaredRuntime(
      @Nullable XmlFile appengineWebXml);

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
  public abstract boolean isJarOrWarMavenBuild(@NotNull Module module);

  public abstract Optional<String> getServiceNameFromAppYaml(@NotNull String appYamlPath)
      throws MalformedYamlFileException;

  public abstract Optional<FlexibleRuntime> getFlexibleRuntimeFromAppYaml(
      @NotNull String appYamlPathString) throws MalformedYamlFileException;

  /**
   * Gets the service specified in an appengine-web.xml file, in its first found service or module
   * XML tag, associated to the module of {@code deploymentSource}. Service has precedence over
   * module.
   *
   * @return the value of the first found service tag, or else the value of the first found module
   * tag, or else "default"
   */
  public abstract String getServiceNameFromAppEngineWebXml(
      Project project, DeploymentSource deploymentSource);


  /**
   * Generates an app.yaml configuration file in the <@code>outputFolderPath</@code>. If an app.yaml
   * already exists it will not overwrite the file.
   */
  public abstract void generateAppYaml(FlexibleRuntime runtime, Module module, Path outputFolderPath);


  /**
   * Generates a Dockerfile in the <@code>outputFolderPath</@code>. If a Dockerfile already exists
   * it will not overwrite the file.
   */
  public abstract void generateDockerfile(AppEngineFlexibleDeploymentArtifactType type,
      Module module, Path outputFolderPath);

  /**
   * Returns the default location of the app.yaml configuration file, relative to a module content
   * root location.
   */
  public abstract String getDefaultAppYamlPath(String moduleRoot);

  /**
   * Returns the default directory of the Dockerfile configuration file, relative to a module
   * content root location.
   */
  public abstract String getDefaultDockerfileDirectory(String moduleRoot);
}
