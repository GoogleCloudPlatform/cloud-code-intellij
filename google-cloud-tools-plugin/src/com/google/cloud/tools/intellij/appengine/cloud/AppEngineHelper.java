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

import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import java.io.File;

/**
 * Provides basic Gcloud based App Engine functionality for our Cloud Tools plugin.
 */
public interface AppEngineHelper {

  /**
   * The path to the gcloud command on the local file system.
   */
  File getGcloudCommandPath();

  /**
   * The default app.yaml to use.
   */
  File defaultAppYaml();

  /**
   * The default Dockerfile we suggest for custom MVM deployments.
   *
   * @param deploymentArtifactType depending on the artifact type we provide a different default
   *                               Dockerfile
   * @return A {@link java.io.File} path to the default Dockerfile
   */
  File defaultDockerfile(DeploymentArtifactType deploymentArtifactType);

  /**
   * Creates a {@link AppEngineDeployAction} that will perform an App Engine flexible environment
   * deployment based on the {@link AppEngineDeploymentConfiguration}.
   *
   * @param loggingHandler logging messages will be output to this
   * @param project the IJ project
   * @param artifactToDeploy the {@link File} path to the Java artifact to be deployed
   * @param deploymentConfiguration the configuration specifying the deployment
   * @param deploymentCallback a callback for handling completions of the operation
   * @return the action that will perform the deployment operation
   */
  AppEngineDeployAction createDeploymentAction(
      LoggingHandler loggingHandler,
      Project project,
      File artifactToDeploy,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      DeploymentOperationCallback deploymentCallback);

  /**
   * Creates a {@link AppEngineStopAction} that will stop an App Engine application that was just
   * deployed.
   *
   * @param loggingHandler logging messages will be output to this
   * @param deploymentConfiguration the configuration specifying the deployment that is to be
   *     stopped
   * @param moduleToStop the module to stop
   * @param versionToStop the version to stop
   * @param undeploymentTaskCallback a callback for handling completions of the operation
   * @return the action that will perform the stop operation
   */
  AppEngineStopAction createStopAction(
      LoggingHandler loggingHandler,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      String moduleToStop,
      String versionToStop,
      UndeploymentTaskCallback undeploymentTaskCallback);
}
