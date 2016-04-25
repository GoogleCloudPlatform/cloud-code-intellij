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

import com.google.cloud.tools.intellij.appengine.util.AppEngineUtil;
import com.google.common.base.Preconditions;
import com.google.cloud.tools.intellij.appengine.cloud.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;

import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.Deployment;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

/**
 * A Cloud SDK (gcloud) based implementation of the {@link AppEngineHelper} interface.
 */
public class CloudSdkAppEngineHelper implements AppEngineHelper {

  private static final String DEFAUL_APP_YAML_PATH = "/generation/src/appengine/mvm/app.yaml";
  private static final String DEFAUL_JAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/jar.dockerfile";
  private static final String DEFAUL_WAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/war.dockerfile";

  private final File gcloudCommandPath;
  private final String projectId;
  private final String googleUserName;

  public CloudSdkAppEngineHelper(@NotNull File gcloudCommandPath, @NotNull String projectId,
     @NotNull String googleUserName) {
    this.gcloudCommandPath = gcloudCommandPath;
    this.projectId = projectId;
    this.googleUserName = googleUserName;
  }

  @NotNull
  @Override
  public File getGcloudCommandPath() {
    return gcloudCommandPath;
  }

  @NotNull
  @Override
  public String getProjectId() {
    return projectId;
  }

  @Override
  public String getGoogleUsername() {
    return googleUserName;
  }

  @NotNull
  @Override
  public File defaultAppYaml() {
    return getFileFromResourcePath(DEFAUL_APP_YAML_PATH);
  }

  @Nullable
  @Override
  public File defaultDockerfile(DeploymentArtifactType deploymentArtifactType) {
    switch (deploymentArtifactType) {
      case WAR:
        return getFileFromResourcePath(DEFAUL_WAR_DOCKERFILE_PATH);
      case JAR:
        return getFileFromResourcePath(DEFAUL_JAR_DOCKERFILE_PATH);
      default:
        return null;
    }
  }

  @NotNull
  @Override
  public AppEngineDeployAction createCustomDeploymentAction(
      LoggingHandler loggingHandler,
      Project project,
      File artifactToDeploy,
      File appYamlPath,
      File dockerfilePath,
      String version,
      DeploymentOperationCallback deploymentCallback) {
    return new AppEngineDeployAction(
        this,
        loggingHandler,
        project,
        artifactToDeploy,
        appYamlPath,
        dockerfilePath,
        version,
        wrapCallbackForUsageTracking(deploymentCallback,
            ConfigType.CUSTOM,DeploymentArtifactType.typeForPath(artifactToDeploy))
    );
  }

  @NotNull
  @Override
  public AppEngineDeployAction createAutoDeploymentAction(
      LoggingHandler loggingHandler,
      Project project,
      File artifactToDeploy,
      String version,
      DeploymentOperationCallback deploymentCallback) throws IllegalArgumentException {
    DeploymentArtifactType artifactType = DeploymentArtifactType.typeForPath(artifactToDeploy);
    if (artifactType == DeploymentArtifactType.UNKNOWN) {
      throw new IllegalArgumentException(artifactToDeploy.getPath() + " is not a support artifact "
          + "type for automatic deployment");
    }
    return new AppEngineDeployAction(
        this,
        loggingHandler,
        project,
        artifactToDeploy,
        defaultAppYaml(),
        defaultDockerfile(artifactType),
        version,
        wrapCallbackForUsageTracking(deploymentCallback, ConfigType.AUTO, artifactType)
    );
  }

  @NotNull
  @Override
  public AppEngineStopAction createStopAction(
      LoggingHandler loggingHandler,
      Set<String> modulesToStop,
      String versionToStop,
      UndeploymentTaskCallback undeploymentTaskCallback) {
    return new AppEngineStopAction(
        this,
        loggingHandler,
        modulesToStop,
        versionToStop,
        undeploymentTaskCallback
    );
  }

  @NotNull
  private DeploymentOperationCallback wrapCallbackForUsageTracking(
      final DeploymentOperationCallback deploymentCallback,
      ConfigType deploymentType, DeploymentArtifactType artifactType) {

    StringBuilder labelBuilder = new StringBuilder("deploy.flex");
    switch (deploymentType) {
      case AUTO:
        labelBuilder.append(".auto");
        break;
      case CUSTOM:
        labelBuilder.append(".custom");
        break;
      default:
        throw new AssertionError();
    }
    labelBuilder.append(".java").append(artifactType.toString());

    final String eventLabel = labelBuilder.toString();

    return new DeploymentOperationCallback() {
      @Override
      public Deployment succeeded(@NotNull DeploymentRuntime deploymentRuntime) {
        UsageTrackerProvider.getInstance()
            .trackEvent(GctTracking.CATEGORY, GctTracking.APP_ENGINE, eventLabel, null);
        return deploymentCallback.succeeded(deploymentRuntime);
      }

      @Override
      public void errorOccurred(@NotNull String errorMessage) {
        deploymentCallback.errorOccurred(errorMessage);
      }
    };
  }

  @NotNull
  private File getFileFromResourcePath(String resourcePath) {
    File appYaml;
    try {
      URL resource = this.getClass().getClassLoader().getResource(resourcePath);
      Preconditions
          .checkArgument(resource != null, resourcePath + " is not a valid resource path.");
      appYaml = new File(resource.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return appYaml;
  }

}
