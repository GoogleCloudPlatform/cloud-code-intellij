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
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkService.SdkStatus;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceManager;
import com.google.cloud.tools.intellij.appengine.sdk.CloudSdkServiceManager.CloudSdkStatusHandler;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

/** A {@link ServerRuntimeInstance} for the {@link AppEngineCloudType}. */
public class AppEngineRuntimeInstance
    extends ServerRuntimeInstance<AppEngineDeploymentConfiguration> {

  private final Multimap<Project, CancellableRunnable> createdDeployments;
  private final ProjectManagerListener projectClosingListener;

  AppEngineRuntimeInstance() {
    createdDeployments = ArrayListMultimap.create();
    projectClosingListener =
        new ProjectManagerListener() {
          @Override
          public void projectClosing(Project project) {
            disconnect();
          }
        };
  }

  @Override
  public void deploy(
      @NotNull final DeploymentTask<AppEngineDeploymentConfiguration> task,
      @NotNull final DeploymentLogManager logManager,
      @NotNull final DeploymentOperationCallback callback) {

    FileDocumentManager.getInstance().saveAllDocuments();

    Services.getLoginService().logInIfNot();
    if (!Services.getLoginService().isLoggedIn()) {
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.not.logged.in"));
      return;
    }

    AppEngineDeploymentConfiguration deploymentConfig = task.getConfiguration();
    AppEngineHelper appEngineHelper = new CloudSdkAppEngineHelper(task.getProject());
    ProjectManager.getInstance()
        .addProjectManagerListener(task.getProject(), projectClosingListener);

    appEngineHelper
        .createDeployRunner(
            logManager.getMainLoggingHandler(), task.getSource(), deploymentConfig, callback)
        .ifPresent(
            deployRunner -> {
              // keep track of any active deployments
              synchronized (createdDeployments) {
                createdDeployments.put(task.getProject(), deployRunner);
              }

              CloudSdkServiceManager.getInstance()
                  .runWhenSdkReady(
                      task.getProject(),
                      deployRunner,
                      GctBundle.message("appengine.deployment.status.deploying"),
                      new CloudSdkStatusHandler() {
                        @Override
                        public void log(String message) {
                          logManager.getMainLoggingHandler().print(message + "\n");
                        }

                        @Override
                        public void onError(String message) {
                          callback.errorOccurred(message);
                        }

                        @Override
                        public void onUserCancel() {
                          String cancelMessage =
                              GctBundle.message("appengine.deployment.user.cancel");
                          callback.errorOccurred(cancelMessage);
                          logManager.getMainLoggingHandler().print(cancelMessage + "\n");
                        }

                        @Override
                        public String getErrorMessage(SdkStatus sdkStatus) {
                          switch (sdkStatus) {
                            case INVALID:
                              return GctBundle.message("appengine.deployment.error.sdk.invalid");
                            case NOT_AVAILABLE:
                              return GctBundle.message(
                                  "appengine.deployment.error.sdk.not.available");
                            default:
                              return GctBundle.message("appengine.deployment.error.sdk.retry");
                          }
                        }
                      });
            });
  }

  /**
   * Disambiguates running deployment line items by prepending a timestamp. Also appends the cloud
   * project and version id's to the deployment string.
   */
  @NotNull
  @Override
  public String getDeploymentName(
      @NotNull DeploymentSource source, AppEngineDeploymentConfiguration configuration) {
    String deploymentName = String.format("[%s] ", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

    // If its a user specified archive source then we want to label it by the name of the archive
    if (source instanceof UserSpecifiedPathDeploymentSource && source.getFile() != null) {
      deploymentName += source.getFile().getName();
    } else {
      deploymentName += source.getPresentableName();
    }

    if (source instanceof AppEngineDeployable) {
      AppEngineDeployable deployable = (AppEngineDeployable) source;

      if (deployable.getProjectName() != null) {
        deploymentName += ". Project: " + ((AppEngineDeployable) source).getProjectName();
      }

      if (deployable.getVersion() != null) {
        deploymentName += ". Version: " + ((AppEngineDeployable) source).getVersion();
      }
    }

    return deploymentName;
  }

  @Override
  public void computeDeployments(@NotNull ComputeDeploymentsCallback callback) {}

  @Override
  public void disconnect() {
    synchronized (createdDeployments) {
      // Kills any executing deployment actions.
      createdDeployments.values().forEach(CancellableRunnable::cancel);
      createdDeployments
          .keys()
          .forEach(
              project ->
                  ProjectManager.getInstance()
                      .removeProjectManagerListener(project, projectClosingListener));
      createdDeployments.clear();
    }
  }
}
