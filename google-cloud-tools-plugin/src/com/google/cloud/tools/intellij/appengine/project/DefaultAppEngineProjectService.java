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
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineStandardFacetType;
import com.google.cloud.tools.intellij.appengine.facet.standard.AppEngineTemplateGroupDescriptorFactory;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import com.intellij.facet.FacetManager;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import com.intellij.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.ModuleDeploymentSource;
import com.intellij.usageView.UsageInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Implementation of methods for inspecting an App Engine project's structure and configuration.
 */
public class DefaultAppEngineProjectService extends AppEngineProjectService {

  private static Logger logger = Logger.getInstance(DefaultAppEngineProjectService.class);

  private static final String RUNTIME_TAG_NAME = "runtime";
  private static final String SERVICE_TAG_NAME = "service";
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
    if (hasAppEngineStandardFacet(module)) {
      if (isFlexCompat(AppEngineAssetProvider.getInstance().loadAppEngineStandardWebXml(
          module.getProject(), ImmutableList.of(module)))) {
        return Optional.of(AppEngineEnvironment.APP_ENGINE_FLEX_COMPAT);
      }

      return Optional.of(AppEngineEnvironment.APP_ENGINE_STANDARD);
    }

    if (hasAppEngineFlexFacet(module)) {
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
  public boolean hasAppEngineStandardFacet(@NotNull Module module) {
    return FacetManager.getInstance(module).getFacetByType(AppEngineStandardFacetType.ID) != null;
  }

  @Override
  public boolean hasAppEngineFlexFacet(@NotNull Module module) {
    return FacetManager.getInstance(module).getFacetByType(AppEngineFlexibleFacetType.ID) != null;
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
    String runtime = rootTag.getSubTagText(RUNTIME_TAG_NAME);
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

  /**
   * @throws MalformedYamlFileException when an app.yaml isn't syntactically well formed
   */
  @Override
  public Optional<String> getServiceNameFromAppYaml(@NotNull String appYamlPathString)
      throws MalformedYamlFileException {
    return getValueFromAppYaml(appYamlPathString, SERVICE_TAG_NAME);
  }

  /**
   * @throws MalformedYamlFileException when an app.yaml isn't syntactically well formed
   */
  @Override
  public Optional<FlexibleRuntime> getFlexibleRuntimeFromAppYaml(
      @NotNull String appYamlPathString) throws MalformedYamlFileException {
    try {
      return getValueFromAppYaml(appYamlPathString, RUNTIME_TAG_NAME)
          .map(String::toUpperCase)
          .map(FlexibleRuntime::valueOf);
    } catch (IllegalArgumentException iae) {
      return Optional.empty();
    }
  }

  /**
   * Returns the value of a key-value pair for a given {@code key}, on the file located at
   * {@code appYamlPathString}.
   * @return a String with the value, or an empty Optional if app.yaml isn't a regular file, or
   * if there is any error getting the value
   * @throws MalformedYamlFileException when an app.yaml isn't syntactically well formed
   */
  private Optional<String> getValueFromAppYaml(@NotNull String appYamlPathString,
      @NotNull String key) throws MalformedYamlFileException {
    Yaml yamlParser = new Yaml();

    Path appYamlPath = Paths.get(appYamlPathString);
    if (!Files.isRegularFile(appYamlPath)) {
      return Optional.empty();
    }

    try (BufferedReader reader = Files.newBufferedReader(appYamlPath, Charset.defaultCharset())) {
      Object parseResult = yamlParser.load(reader);

      if (!(parseResult instanceof Map)) {
        return Optional.empty();
      }

      // It's possible to get rid of this unchecked cast using a loadAs(file,
      // AppEngineYamlWebApp.class) sort of approach.
      Map<String, String> yamlMap = (Map<String, String>) parseResult;

      return yamlMap.containsKey(key) ? Optional.of(yamlMap.get(key)) : Optional.empty();
    } catch (ScannerException se) {
      throw new MalformedYamlFileException(se);
    } catch (InvalidPathException | IOException ioe) {
      return Optional.empty();
    }
  }

  @Override
  public String getServiceNameFromAppEngineWebXml(
      Project project, DeploymentSource deploymentSource) {
    XmlFile appengineWebXml = loadAppEngineStandardWebXml(project, deploymentSource);

    return getServiceNameFromAppEngineWebXml(appengineWebXml);
  }

  @VisibleForTesting
  String getServiceNameFromAppEngineWebXml(XmlFile appengineWebXml) {

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
  public void generateAppYaml(FlexibleRuntime runtime, Module module, Path outputFolderPath) {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_FLEX_APP_YAML_CREATE)
        .ping();

    Properties templateProperties =
        FileTemplateManager.getDefaultInstance().getDefaultProperties();
    templateProperties.put("RUNTIME", runtime.toString());

    ApplicationManager.getApplication().runWriteAction(() -> {
      PsiElement element = generateFromTemplate(
          AppEngineTemplateGroupDescriptorFactory.APP_YAML_TEMPLATE,
          "app.yaml",
          outputFolderPath,
          templateProperties,
          module);

      if (element == null) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_FLEX_APP_YAML_CREATE_FAIL)
            .ping();
      } else {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_FLEX_APP_YAML_CREATE_SUCCESS)
            .ping();
      }
    });
  }

  @Override
  public void generateDockerfile(AppEngineFlexibleDeploymentArtifactType type, Module module,
      Path outputFolderPath) {
    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.APP_ENGINE_FLEX_DOCKERFILE_CREATE)
        .ping();

    if (type == AppEngineFlexibleDeploymentArtifactType.UNKNOWN) {
      throw new RuntimeException("Cannot generate Dockerfile for unknown artifact type.");
    }

    ApplicationManager.getApplication().runWriteAction(() -> {
      PsiElement element = generateFromTemplate(
          type == AppEngineFlexibleDeploymentArtifactType.JAR
              ? AppEngineTemplateGroupDescriptorFactory.DOCKERFILE_JAR_TEMPLATE
              : AppEngineTemplateGroupDescriptorFactory.DOCKERFILE_WAR_TEMPLATE,
          "Dockerfile",
          outputFolderPath,
          FileTemplateManager.getDefaultInstance().getDefaultProperties(),
          module);

      if (element != null) {
        // Remove the .docker extension to satisfy the Docker convention. This extension was added
        // since the templating mechanism requires an extension or else a default template type of
        // "java" will be assumed.
        RenamePsiElementProcessor.DEFAULT.renameElement(
            element,
            "Dockerfile" /*newName*/,
            UsageInfo.EMPTY_ARRAY,
            null /*listener*/);

        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_FLEX_DOCKERFILE_CREATE_SUCCESS)
            .ping();
      } else {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.APP_ENGINE_FLEX_DOCKERFILE_CREATE_FAIL)
            .ping();
      }
    });
  }

  @Nullable
  private PsiElement generateFromTemplate(String templateName, String outputFileName,
      Path outputFolderPath, Properties templateProperties, Module module) {
    FileTemplate configTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(
        templateName);

    File outputFolder = outputFolderPath.toFile();
    if (!outputFolder.exists() && !outputFolder.mkdirs()) {
      logger.warn("Failed to create " + outputFileName + " directory: " + outputFolder.toString());
      return null;
    }
    VirtualFile virtualOutputFolder = LocalFileSystem.getInstance().
        refreshAndFindFileByIoFile(outputFolder);
    if (virtualOutputFolder == null) {
      logger.warn("Failed to locate " + outputFolder.toString() + "directory");
      return null;
    }
    PsiDirectory outputPsiDirectory = PsiManager.getInstance(module.getProject())
        .findDirectory(virtualOutputFolder);

    if (outputPsiDirectory != null
        && FileTemplateUtil.canCreateFromTemplate(
        new PsiDirectory[]{outputPsiDirectory}, configTemplate)) {
      try {
        return FileTemplateUtil.createFromTemplate(
            configTemplate,
            outputFileName,
            templateProperties,
            outputPsiDirectory);
      } catch (Exception e) {
        // If the file already exists, this exception will be thrown by createFromTemplate
        // We want to silently skip the generation in this case.
        logger.debug("Failed to create app.yaml from template. " + e.getMessage());
      }
    } else {
      logger.error("Failed to create app.yaml from template");
    }

    return null;
  }

  @Override
  public String getDefaultAppYamlPath(String moduleRoot) {
    return moduleRoot + "/src/main/appengine/app.yaml";
  }

  @Override
  public String getDefaultDockerDirectory(String moduleRoot) {
    return moduleRoot + "/src/main/docker";
  }
}
