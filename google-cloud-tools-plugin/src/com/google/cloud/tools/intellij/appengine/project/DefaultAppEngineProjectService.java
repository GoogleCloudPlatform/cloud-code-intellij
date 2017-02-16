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
import com.google.cloud.tools.intellij.appengine.cloud.standard.AppEngineStandardRuntime;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacet;
import com.google.common.collect.ImmutableList;

import com.intellij.facet.FacetManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

/**
 * Implementation of methods for inspecting an App Engine project's structure and configuration.
 */
public class DefaultAppEngineProjectService extends AppEngineProjectService {

  private static final String AE_WEB_XML_RUNTIME_TAG = "runtime";
  private static final String SERVICE_TAG_YAML = "service:";
  private static final String RUNTIME_TAG_YAML = "runtime:";
  private static final String JAVA_RUNTIME = "java";
  private static final String CUSTOM_RUNTIME = "custom";
  private static final String DEFAULT_SERVICE = "default";

  private AppEngineAssetProvider assetProvider;

  DefaultAppEngineProjectService() {
    assetProvider = AppEngineAssetProvider.getInstance();
  }

  @Override
  public boolean isFlexCompat(@NotNull Project project, @NotNull DeploymentSource source) {
    XmlFile appEngineWebXml = loadAppEngineStandardWebXml(project, source);

    return appEngineWebXml != null && isFlexCompat(appEngineWebXml);
  }

  @Override
  public boolean isFlexCompat(@Nullable XmlFile appEngineWebXml) {
    if (appEngineWebXml == null) {
      return false;
    }

    XmlTag compatConfig = getFlexCompatXmlConfiguration(appEngineWebXml);

    return isFlexCompatEnvFlex(compatConfig) || isFlexCompatVmTrue(compatConfig);
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

  @Override
  public Optional<AppEngineEnvironment> getModuleAppEngineEnvironment(Module module) {
    // The order here is important -- Standard must come before Flexible so that when both Standard
    // and Flexible are selected from the New Project/Module dialog, Standard takes precedence.
    if (FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacet.ID) != null) {
      if (isFlexCompat(AppEngineAssetProvider.getInstance().loadAppEngineStandardWebXml(
          module.getProject(), ImmutableList.of(module)))) {
        return Optional.of(AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT);
      }

      return Optional.of(AppEngineEnvironment.APP_ENGINE_STANDARD);
    }

    if (FacetManager.getInstance(module).getFacetByType(AppEngineFlexibleFacetType.ID) != null) {
      return Optional.of(AppEngineEnvironment.APP_ENGINE_FLEX);
    }

    return Optional.empty();
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
  public boolean isMavenModule(@NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(module.getProject());
    MavenProject mavenProject = projectsManager.findProject(module);

    return mavenProject != null
        && projectsManager.isMavenizedModule(module);
  }

  @Override
  public boolean isGradleModule(@NotNull Module module) {
    return ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module);
  }

  @Override
  public boolean isJarOrWarMavenBuild(@NotNull Module module) {
    MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(module.getProject());
    MavenProject mavenProject = projectsManager.findProject(module);

    return mavenProject != null
        && isMavenModule(module)
        && ("jar".equalsIgnoreCase(mavenProject.getPackaging())
        || "war".equalsIgnoreCase(mavenProject.getPackaging()));
  }

  @Nullable
  private XmlFile loadAppEngineStandardWebXml(@NotNull Project project,
      @Nullable DeploymentSource source) {
    if (source instanceof ArtifactDeploymentSource) {
      Artifact artifact = ((ArtifactDeploymentSource) source).getArtifact();
      return artifact != null
          ? assetProvider.loadAppEngineStandardWebXml(project, artifact)
          : null;
    } else if (source instanceof ModuleDeploymentSource) {
      Module module = ((ModuleDeploymentSource) source).getModule();
      return module != null
          ? assetProvider.loadAppEngineStandardWebXml(project, Collections.singletonList(module))
          : null;
    }

    return null;
  }

  /**
   * Given an artifact, returns the xml tag corresponding to the artifact's
   * appengine-web.xml compat configuration or null if there isn't one.
   */
  @Nullable
  private XmlTag getFlexCompatXmlConfiguration(@Nullable XmlFile webXml) {
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

  @Override
  @Nullable
  public AppEngineStandardRuntime getAppEngineStandardDeclaredRuntime(
      @Nullable XmlFile appengineWebXml) {
    XmlTag rootTag;
    if (appengineWebXml == null || (rootTag = appengineWebXml.getRootTag()) == null) {
      return null;
    }
    String runtime = rootTag.getSubTagText(AE_WEB_XML_RUNTIME_TAG);
    if (runtime == null) {
      return null;
    }

    try {
      return AppEngineStandardRuntime.fromLabel(runtime);
    } catch (IllegalArgumentException exception) {
      // the declared runtime version is invalid, nothing we can do here
      return null;
    }
  }

  @Override
  public Optional<String> getServiceNameFromAppYaml(@NotNull String appYamlPathString) {
    return getValueFromAppYaml(appYamlPathString, SERVICE_TAG_YAML);
  }

  @Override
  public FlexibleRuntime getFlexibleRuntimeFromAppYaml(@NotNull String appYamlPathString) {
    return getValueFromAppYaml(appYamlPathString, RUNTIME_TAG_YAML).map(runtimeValue -> {
      if (runtimeValue.equals(JAVA_RUNTIME)) {
        return FlexibleRuntime.JAVA;
      }
      if (runtimeValue.equals(CUSTOM_RUNTIME)) {
        return FlexibleRuntime.CUSTOM;
      }
      return FlexibleRuntime.UNKNOWN;
    }).orElse(FlexibleRuntime.UNKNOWN);
  }

  private Optional<String> getValueFromAppYaml(@NotNull String appYamlPathString,
      @NotNull String key) {
    Path appYamlPath;
    try {
      appYamlPath = Paths.get(appYamlPathString);
    } catch (InvalidPathException ipe) {
      return Optional.empty();
    }

    try {
      return Files.readAllLines(appYamlPath).stream()
          .filter(line -> line.startsWith(key))
          .map(line -> line.substring(key.length()).trim())
          .findFirst();
    } catch (IOException ioe) {
      return Optional.empty();
    }
  }

  @Override
  public String getServiceNameFromAppEngineWebXml(
      Project project, DeploymentSource deploymentSource) {
    XmlFile appengineWebXml = loadAppEngineStandardWebXml(project, deploymentSource);

    if (appengineWebXml != null) {
      XmlTag root = appengineWebXml.getRootTag();
      if (root != null) {
        XmlTag serviceTag = root.findFirstSubTag("service");
        if (serviceTag != null) {
          return serviceTag.getValue().getText();
        }
        XmlTag moduleTag = root.findFirstSubTag("module");
        if (moduleTag != null) {
          return moduleTag.getValue().getText();
        }
      }
    }

    return DEFAULT_SERVICE;
  }

  @Override
  public String getDefaultAppYamlPath(String moduleRoot) {
    return moduleRoot + "/src/main/appengine/app.yaml";
  }

  @Override
  public String getDefaultDockerfilePath(String moduleRoot) {
    return moduleRoot + "/src/main/docker/Dockerfile";
  }
}
