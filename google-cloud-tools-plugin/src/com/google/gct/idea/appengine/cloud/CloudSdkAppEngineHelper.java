/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.gct.idea.appengine.cloud;

import com.google.common.base.Preconditions;

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

  private static final String DEFAUL_APP_YAML_PATH = "/generation/src/appengine/mvm/app.yaml";
  private static final String DEFAUL_JAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/jar.dockerfile";
  private static final String DEFAUL_WAR_DOCKERFILE_PATH
      = "/generation/src/appengine/mvm/war.dockerfile";

  private final File gcloudCommandPath;
  private final String projectId;

  public CloudSdkAppEngineHelper(@NotNull File gcloudCommandPath, @NotNull String projectId) {
    this.gcloudCommandPath = gcloudCommandPath;
    this.projectId = projectId;
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
  public Runnable createCustomDeploymentOperation(LoggingHandler loggingHandler,
      File artifactToDeploy, File appYamlPath, File dockerfilePath,
      DeploymentOperationCallback deploymentCallback) {
    return new DoManagedVmDeployment(
        this,
        loggingHandler,
        artifactToDeploy,
        appYamlPath,
        dockerfilePath,
        deploymentCallback
    );
  }

  @NotNull
  @Override
  public Runnable createAutoDeploymentOperation(
      LoggingHandler loggingHandler,
      File artifactToDeploy,
      DeploymentOperationCallback deploymentCallback) throws IllegalArgumentException {
    DeploymentArtifactType artifactType = DeploymentArtifactType.typeForPath(artifactToDeploy);
    if (artifactType == DeploymentArtifactType.UNKNOWN) {
      throw new IllegalArgumentException(artifactToDeploy.getPath() + " is not a support artifact "
          + "type for automatic deployment");
    }
    return new DoManagedVmDeployment(
        this,
        loggingHandler,
        artifactToDeploy,
        defaultAppYaml(),
        defaultDockerfile(artifactType),
        deploymentCallback
    );
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
