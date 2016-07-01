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

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of methods for inspecting an App Engine project's structure and configuration.
 */
public class DefaultAppEngineProjectService extends AppEngineProjectService {

  private AppEngineAssetProvider assetProvider;

  public DefaultAppEngineProjectService() {
    assetProvider = AppEngineAssetProvider.getInstance();
  }

  @Override
  public boolean isFlexCompat(@NotNull Project project, @NotNull DeploymentSource source) {
    Artifact artifact = getArtifact(source);

    return artifact != null && isFlexCompat(project, artifact);
  }

  @Override
  public boolean isFlexCompat(@NotNull Project project, @NotNull Artifact artifact) {
    XmlTag compatConfig = getFlexCompatXmlConfiguration(project, artifact);

    return isFlexCompatEnvFlex(compatConfig) || isFlexCompatVmTrue(compatConfig);
  }

  @Override
  public boolean isFlexCompatEnvFlex(@NotNull Project project, @NotNull DeploymentSource source) {
    XmlTag compatConfig = getFlexCompatXmlConfiguration(project, getArtifact(source));

    return isFlexCompatEnvFlex(compatConfig);
  }

  private boolean isFlexCompatEnvFlex(@Nullable XmlTag compatConfig) {
    return compatConfig != null
        && "env".equalsIgnoreCase(compatConfig.getName())
        && "flex".equalsIgnoreCase(compatConfig.getValue().getTrimmedText());
  }

  private boolean isFlexCompatVmTrue(@Nullable XmlTag compatConfig) {
    return compatConfig != null
        && "vm".equalsIgnoreCase(compatConfig.getName())
        && Boolean.parseBoolean(compatConfig.getValue().getTrimmedText());
  }

  @Nullable
  @Override
  public AppEngineEnvironment getAppEngineArtifactEnvironment(@NotNull Project project,
      @NotNull Artifact artifact) {
    if (isAppEngineStandardArtifact(project, artifact)) {
      return isFlexCompat(project, artifact)
          ? AppEngineEnvironment.APP_ENGINE_FLEX
          : AppEngineEnvironment.APP_ENGINE_STANDARD;
    } else if (isAppEngineFlexArtifactType(artifact)) {
      return AppEngineEnvironment.APP_ENGINE_FLEX;
    } else {
      return null;
    }
  }

  @Override
  public boolean containsAppEngineStandardArtifacts(@NotNull Project project,
      @NotNull Module module) {
    return containsAppEngineStandardArtifacts(
        project, ArtifactUtil.getArtifactsContainingModuleOutput(module));
  }

  @Override
  public boolean containsAppEngineStandardArtifacts(Project project,
      Collection<Artifact> artifacts) {
    for (Artifact artifact : artifacts) {
      if (hasAppEngineStandardFacet(project, artifact)
          && isAppEngineStandardArtifactType(artifact)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isAppEngineStandardArtifact(@NotNull Project project, @NotNull Artifact artifact) {
    return hasAppEngineStandardFacet(project, artifact)
        && isAppEngineStandardArtifactType(artifact);
  }

  @Override
  public boolean hasAppEngineStandardFacet(@NotNull Project project, @NotNull Artifact artifact) {
    Set<Module> modules = ArtifactUtil
        .getModulesIncludedInArtifacts(Collections.singletonList(artifact), project);

    for (Module module : modules) {
      for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
        if (facet != null && APP_ENGINE_STANDARD_FACET_NAME.equals(facet.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean isAppEngineStandardArtifactType(@NotNull Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "exploded-war".equalsIgnoreCase(artifactId);
  }

  @Override
  public boolean isAppEngineFlexArtifactType(@NotNull Artifact artifact) {
    String artifactId = artifact.getArtifactType().getId();
    return "jar".equalsIgnoreCase(artifactId) || "war".equals(artifactId);
  }

  @Override
  public boolean isJarOrWarMavenBuild(@NotNull Project project, @NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
    MavenProject mavenProject = projectsManager.findProject(module);

    boolean isMavenProject = projectsManager.isMavenizedModule(module)
        && mavenProject != null;

    return isMavenProject
        && ("jar".equalsIgnoreCase(mavenProject.getPackaging())
        || "war".equalsIgnoreCase(mavenProject.getPackaging()));
  }

  /**
   * Given an artifact, returns the xml tag corresponding to the artifact's
   * appengine-web.xml compat configuration or null if there isn't one.
   */
  @Nullable
  private XmlTag getFlexCompatXmlConfiguration(@NotNull Project project,
      @Nullable Artifact artifact) {
    if (artifact == null || !isAppEngineStandardArtifact(project, artifact)) {
      return null;
    }

    XmlFile webXml = assetProvider.loadAppEngineStandardWebXml(project, artifact);

    if (webXml != null) {
      XmlTag root = webXml.getRootTag();
      if (root != null) {
        XmlTag vmTag = root.findFirstSubTag("vm");
        if (vmTag != null) {
          return vmTag;
        } else {
          return root.findFirstSubTag("env");
        }
      }
    }

    return null;
  }

  @Nullable
  private static Artifact getArtifact(@NotNull DeploymentSource source) {
    if (source instanceof ArtifactDeploymentSource) {
      return ((ArtifactDeploymentSource) source).getArtifact();
    }

    return null;
  }
}
