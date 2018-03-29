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

import com.google.cloud.tools.intellij.appengine.cloud.flexible.UserSpecifiedPathDeploymentSource;
import com.google.cloud.tools.intellij.appengine.facet.flexible.AppEngineFlexibleFacet;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService;
import com.google.cloud.tools.intellij.appengine.project.AppEngineProjectService.FlexibleRuntime;
import com.google.cloud.tools.intellij.appengine.project.MalformedYamlFileException;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidationResult;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkValidator;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.collect.Iterables;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.util.CloudDeploymentNameConfiguration;
import com.intellij.util.xmlb.annotations.Attribute;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 * The model for an App Engine based deployment configuration. This state is specific to the
 * artifact that's being deployed, as such there can be multiple per project.
 */
public class AppEngineDeploymentConfiguration
    extends CloudDeploymentNameConfiguration<AppEngineDeploymentConfiguration> {

  public static final String USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE = "userSpecifiedArtifactPath";
  public static final String STAGED_ARTIFACT_NAME = "stagedArtifactName";
  public static final String STAGED_ARTIFACT_NAME_LEGACY = "stagedArtifactNameLegacy";
  public static final String ENVIRONMENT_ATTRIBUTE = "environment";

  private static final String DOCKERFILE_NAME = "Dockerfile";

  private String cloudProjectName;
  private String googleUsername;

  /**
   * Environment is stored here in order to restore the environment when this configuration is
   * deserialized. At deserialization time, we cannot resolve the environment via the normal means
   * of inspecting the Project's modules and artifacts because this happens before the modules have
   * been loaded.
   */
  private AppEngineEnvironment environment;

  private String userSpecifiedArtifactPath;
  private boolean promote;
  private boolean stopPreviousVersion;
  private String version;
  private boolean deployAllConfigs;
  // Used to resolve the facet configuration for flexible deployments
  private String moduleName;
  private String stagedArtifactName;
  private boolean stagedArtifactNameLegacy;

  @Attribute("cloudProjectName")
  public String getCloudProjectName() {
    return cloudProjectName;
  }

  @Attribute("googleUsername")
  public String getGoogleUsername() {
    return googleUsername;
  }

  @Attribute(ENVIRONMENT_ATTRIBUTE)
  public AppEngineEnvironment getEnvironment() {
    return environment;
  }

  @Attribute(USER_SPECIFIED_ARTIFACT_PATH_ATTRIBUTE)
  public String getUserSpecifiedArtifactPath() {
    return userSpecifiedArtifactPath;
  }

  @Attribute("promote")
  public boolean isPromote() {
    return promote;
  }

  @Attribute("stopPreviousVersion")
  public boolean isStopPreviousVersion() {
    return stopPreviousVersion;
  }

  @Attribute("version")
  public String getVersion() {
    return version;
  }

  @Attribute("deployAllConfigs")
  public boolean isDeployAllConfigs() {
    return deployAllConfigs;
  }

  @Attribute("moduleName")
  public String getModuleName() {
    return moduleName;
  }

  @Attribute(STAGED_ARTIFACT_NAME)
  public String getStagedArtifactName() {
    return stagedArtifactName;
  }

  @Attribute(STAGED_ARTIFACT_NAME_LEGACY)
  public boolean isStagedArtifactNameLegacy() {
    return stagedArtifactNameLegacy;
  }

  public void setDeployAllConfigs(boolean deployAllConfigs) {
    this.deployAllConfigs = deployAllConfigs;
  }

  public void setCloudProjectName(String cloudProjectName) {
    this.cloudProjectName = cloudProjectName;
  }

  public void setGoogleUsername(String googleUsername) {
    this.googleUsername = googleUsername;
  }

  public void setEnvironment(AppEngineEnvironment environment) {
    this.environment = environment;
  }

  public void setUserSpecifiedArtifactPath(String userSpecifiedArtifactPath) {
    this.userSpecifiedArtifactPath = userSpecifiedArtifactPath;
  }

  public void setPromote(boolean promote) {
    this.promote = promote;
  }

  public void setStopPreviousVersion(boolean stopPreviousVersion) {
    this.stopPreviousVersion = stopPreviousVersion;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setModuleName(String moduleName) {
    this.moduleName = moduleName;
  }

  public void setStagedArtifactName(String stagedArtifactName) {
    this.stagedArtifactName = stagedArtifactName;
  }

  public void setStagedArtifactNameLegacy(boolean stagedArtifactNameLegacy) {
    this.stagedArtifactNameLegacy = stagedArtifactNameLegacy;
  }

  @Override
  public void checkConfiguration(
      RemoteServer<?> server, DeploymentSource deploymentSource, Project project)
      throws RuntimeConfigurationException {
    if (!(deploymentSource instanceof AppEngineDeployable)) {
      throw new RuntimeConfigurationError(
          GctBundle.message("appengine.deployment.invalid.source.error"));
    }

    AppEngineDeployable deployable = (AppEngineDeployable) deploymentSource;
    checkCommonConfig(deployable);
    if (deployable.getEnvironment() != null && deployable.getEnvironment().isFlexible()) {
      checkFlexConfig(deployable, project);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (!(other instanceof AppEngineDeploymentConfiguration)) {
      return false;
    }

    AppEngineDeploymentConfiguration otherConfig = (AppEngineDeploymentConfiguration) other;
    return Objects.equals(cloudProjectName, otherConfig.cloudProjectName)
        && Objects.equals(googleUsername, otherConfig.googleUsername)
        && Objects.equals(environment, otherConfig.environment)
        && Objects.equals(userSpecifiedArtifactPath, otherConfig.userSpecifiedArtifactPath)
        && Objects.equals(promote, otherConfig.promote)
        && Objects.equals(stopPreviousVersion, otherConfig.stopPreviousVersion)
        && Objects.equals(version, otherConfig.version)
        && Objects.equals(deployAllConfigs, otherConfig.deployAllConfigs)
        && Objects.equals(moduleName, otherConfig.moduleName)
        && Objects.equals(stagedArtifactName, otherConfig.stagedArtifactName)
        && Objects.equals(stagedArtifactNameLegacy, otherConfig.stagedArtifactNameLegacy)
        && Objects.equals(isDefaultDeploymentName(), otherConfig.isDefaultDeploymentName())
        && Objects.equals(getDeploymentName(), otherConfig.getDeploymentName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cloudProjectName,
        googleUsername,
        environment,
        userSpecifiedArtifactPath,
        promote,
        stopPreviousVersion,
        version,
        deployAllConfigs,
        moduleName,
        stagedArtifactName,
        stagedArtifactNameLegacy,
        isDefaultDeploymentName(),
        getDeploymentName());
  }

  private void checkCommonConfig(AppEngineDeployable deployable) throws RuntimeConfigurationError {
    // do not check SDK if it supports dynamic install - the deployment runner will block itself
    // until installation is done.
    CloudSdkService cloudSdkService = CloudSdkService.getInstance();
    SdkStatus sdkStatus = cloudSdkService.getStatus();
    if (sdkStatus != SdkStatus.READY && !cloudSdkService.isInstallSupported()) {
      Set<CloudSdkValidationResult> sdkValidationResult =
          CloudSdkValidator.getInstance().validateCloudSdk();
      if (!sdkValidationResult.isEmpty()) {
        CloudSdkValidationResult result = Iterables.getFirst(sdkValidationResult, null);
        throw new RuntimeConfigurationError(
            GctBundle.message("appengine.flex.config.server.error", result.getMessage()));
      }
    }

    check(
        deployable instanceof UserSpecifiedPathDeploymentSource || deployable.isValid(),
        "appengine.config.deployment.source.error");
    check(
        StringUtils.isNotBlank(cloudProjectName), "appengine.flex.config.project.missing.message");
  }

  /**
   * Checks that this configuration is valid for a flex deployment, otherwise throws a {@link
   * RuntimeConfigurationError}.
   *
   * @param deployable the {@link AppEngineDeployable deployment source} that was selected by the
   *     user to deploy
   * @param project the {@link Project} that this configuration belongs to
   * @throws RuntimeConfigurationError if this configuration is not valid for a flex deployment
   */
  private void checkFlexConfig(AppEngineDeployable deployable, Project project)
      throws RuntimeConfigurationError {
    check(
        !(deployable instanceof UserSpecifiedPathDeploymentSource)
            || (!StringUtil.isEmpty(userSpecifiedArtifactPath)
                && isJarOrWar(userSpecifiedArtifactPath)),
        "appengine.flex.config.user.specified.artifact.error");
    check(StringUtils.isNotBlank(moduleName), "appengine.flex.config.select.module");

    AppEngineFlexibleFacet facet = AppEngineFlexibleFacet.getFacetByModuleName(moduleName, project);
    check(facet != null, "appengine.flex.config.select.module");

    String appYamlPath = facet.getConfiguration().getAppYamlPath();
    check(StringUtils.isNotBlank(appYamlPath), "appengine.flex.config.browse.app.yaml");
    check(Files.exists(Paths.get(appYamlPath)), "appengine.deployment.config.appyaml.error");

    try {
      Optional<FlexibleRuntime> runtime =
          AppEngineProjectService.getInstance().getFlexibleRuntimeFromAppYaml(appYamlPath);
      if (runtime.isPresent() && runtime.get().isCustom()) {
        String dockerDirectory = facet.getConfiguration().getDockerDirectory();
        check(
            StringUtils.isNotBlank(dockerDirectory),
            "appengine.flex.config.browse.docker.directory");
        check(
            Files.exists(Paths.get(dockerDirectory, DOCKERFILE_NAME)),
            "appengine.deployment.config.dockerfile.error");
      }
    } catch (MalformedYamlFileException e) {
      throw new RuntimeConfigurationError(GctBundle.message("appengine.appyaml.malformed"));
    }
  }

  /**
   * Ensures the truth of the given boolean expression, otherwise throws a {@link
   * RuntimeConfigurationError} with the given message.
   *
   * @param expression the expression to test
   * @param message the key of the message (as described by {@link GctBundle#message}) to show in
   *     the error
   * @throws RuntimeConfigurationError if the given expression is false
   */
  private static void check(boolean expression, String message) throws RuntimeConfigurationError {
    if (!expression) {
      throw new RuntimeConfigurationError(GctBundle.message(message));
    }
  }

  /**
   * Returns true if the given path points to a valid JAR or WAR file, otherwise returns false.
   *
   * @param stringPath the path to check
   */
  private static boolean isJarOrWar(String stringPath) {
    String lowercasePath = stringPath.toLowerCase();
    return Files.isRegularFile(Paths.get(stringPath))
        && (lowercasePath.endsWith(".jar") || lowercasePath.endsWith(".war"));
  }
}
