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

package com.google.cloud.tools.intellij.appengine.action;

import com.google.cloud.tools.intellij.appengine.action.configuration.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.appengine.action.configuration.AppEngineDeploymentConfiguration.ConfigType;
import com.google.cloud.tools.intellij.appengine.cloud.DeploymentArtifactType;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.google.common.base.Preconditions;

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

/**
 * A Cloud SDK (gcloud) based implementation of the {@link AppEngineHelper} interface.
 */
public class CloudSdkAppEngineHelper implements AppEngineHelper {

  private static final String DEFAULT_APP_YAML_PATH = "/generation/src/appengine/mvm/app.yaml";
  private static final String DEFAULT_JAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/jar.dockerfile";
  private static final String DEFAULT_WAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/war.dockerfile";

  private final File gcloudCommandPath;

  public CloudSdkAppEngineHelper(@NotNull File gcloudCommandPath) {
    this.gcloudCommandPath = gcloudCommandPath;
  }

  @NotNull
  @Override
  public File getGcloudCommandPath() {
    return gcloudCommandPath;
  }

  @NotNull
  @Override
  public File defaultAppYaml() {
    return getFileFromResourcePath(DEFAULT_APP_YAML_PATH);
  }

  @Nullable
  @Override
  public File defaultDockerfile(DeploymentArtifactType deploymentArtifactType) {
    switch (deploymentArtifactType) {
      case WAR:
        return getFileFromResourcePath(DEFAULT_WAR_DOCKERFILE_PATH);
      case JAR:
        return getFileFromResourcePath(DEFAULT_JAR_DOCKERFILE_PATH);
      default:
        return null;
    }
  }

  @NotNull
  @Override
  public AppEngineDeployAction createDeploymentAction(
      LoggingHandler loggingHandler,
      Project project,
      File artifactToDeploy,
      AppEngineDeploymentConfiguration deploymentConfiguration,
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
        deploymentConfiguration,
        wrapCallbackForUsageTracking(deploymentCallback, ConfigType.AUTO, artifactType)
    );
  }

  @NotNull
  @Override
  public AppEngineStopAction createStopAction(
      LoggingHandler loggingHandler,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      String moduleToStop,
      String versionToStop,
      UndeploymentTaskCallback undeploymentTaskCallback) {
    return new AppEngineStopAction(
        this,
        loggingHandler,
        deploymentConfiguration,
        moduleToStop,
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
    } catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
    return appYaml;
  }

}
