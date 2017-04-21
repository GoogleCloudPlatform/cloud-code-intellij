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

import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance.DeploymentOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Provides basic Gcloud based App Engine functionality for our Cloud Tools plugin.
 */
public interface AppEngineHelper {

  /**
   * The project within the context of this helper.
   */
  Project getProject();

  /**
   * Creates a {@link Runnable} that will execute the tasks necessary for deployment to an App
   * Engine environment.
   *
   * @param loggingHandler logging messages will be output to this
   * @param source the deployment source to be deployed
   * @param deploymentConfiguration the configuration specifying the deployment
   * @param callback a callback for handling completions of the operation
   * @return an {@link Optional} containing the runnable that will perform the deployment operation
   */
  Optional<CancellableRunnable> createDeployRunner(
      LoggingHandler loggingHandler,
      DeploymentSource source,
      AppEngineDeploymentConfiguration deploymentConfiguration,
      DeploymentOperationCallback callback);

  /**
   * Creates a temporary staging directory on the local filesystem.
   *
   * @param loggingHandler logging messages will be output to this
   * @param cloudProjectName the app engine project name
   * @return the file representing the staging directory
   * @throws IOException if the staging fails
   */
  Path createStagingDirectory(
      LoggingHandler loggingHandler,
      String cloudProjectName) throws IOException;

  /**
   * Creates a {@link CloudSdk} object that is used in execution of various App Engine actions.
   *
   * @param loggingHandler logging messages will be output to this
   * @param startListener the "callback" listener used for fetching the running process
   * @param logListener the output listener for handling "normal" operation log messages
   * @param outputListener the output listener for handling the output messages of the operation
   * @param exitListener the listener for handling the completion of the operation
   * @return the {@link CloudSdk} object used in executing the operation
   */
  CloudSdk createSdk(
      LoggingHandler loggingHandler,
      ProcessStartListener startListener,
      ProcessOutputLineListener logListener,
      ProcessOutputLineListener outputListener,
      ProcessExitListener exitListener);

  /**
   * Attempts to locally stage the user credentials to support various App Engine tasks. If not
   * successful then the user is shown a dialog opting to add an account. Then the staging is
   * attempted again.
   */
  Optional<Path> stageCredentials(String googleUsername);

  /**
   * Deletes the locally staged credentials, if they exist.
   */
  void deleteCredentials();
}
