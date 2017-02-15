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
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.base.Strings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vcs.impl.CancellableRunnable;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.runtime.deployment.DeploymentLogManager;
import com.intellij.remoteServer.runtime.deployment.DeploymentTask;
import com.intellij.remoteServer.runtime.deployment.ServerRuntimeInstance;

import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ServerRuntimeInstance} for the {@link AppEngineCloudType}.
 */
public class AppEngineRuntimeInstance extends
    ServerRuntimeInstance<AppEngineDeploymentConfiguration> {

  private final Set<CancellableRunnable> createdDeployments;

  public AppEngineRuntimeInstance() {
    this.createdDeployments = new HashSet<>();
  }

  @Override
  public void deploy(@NotNull final DeploymentTask<AppEngineDeploymentConfiguration> task,
      @NotNull final DeploymentLogManager logManager,
      @NotNull final DeploymentOperationCallback callback) {

    FileDocumentManager.getInstance().saveAllDocuments();

    Services.getLoginService().logInIfNot();
    if (!Services.getLoginService().isLoggedIn()) {
      callback.errorOccurred(GctBundle.message("appengine.deployment.error.not.logged.in"));
      return;
    }

    AppEngineDeploymentConfiguration deploymentConfig = task.getConfiguration();

    AppEngineEnvironment environment =
        AppEngineEnvironment.valueOf(deploymentConfig.getEnvironment());
    if (environment == AppEngineEnvironment.APP_ENGINE_FLEX
        && Strings.isNullOrEmpty(deploymentConfig.getYamlPath())) {
      callback.errorOccurred(GctBundle.getString("appengine.deployment.error.staging.yaml")
          + "\n" + GctBundle.getString("appengine.deployment.error.suggestflexfacet"));
      return;
    }

    AppEngineHelper appEngineHelper = new CloudSdkAppEngineHelper(task.getProject());

    appEngineHelper
        .createDeployRunner(
            logManager.getMainLoggingHandler(), task.getSource(), deploymentConfig, callback)
        .ifPresent(
            deployRunner -> {
              // keep track of any active deployments
              synchronized (createdDeployments) {
                createdDeployments.add(deployRunner);
              }

              ProgressManager.getInstance()
                  .run(
                      new Task.Backgroundable(
                          task.getProject(),
                          GctBundle.message("appengine.deployment.status.deploying"),
                          true,
                          null) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                          ApplicationManager.getApplication().invokeLater(deployRunner);
                        }
                      });
            });
  }

  /**
   * Disambiguates running deployment line items by prepending a timestamp. Also appends the
   * cloud project and version id's to the deployment string.
   */
  @NotNull
  @Override
  public String getDeploymentName(@NotNull DeploymentSource source,
      AppEngineDeploymentConfiguration configuration) {
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
  public void computeDeployments(@NotNull ComputeDeploymentsCallback callback) {
  }

  @Override
  public void disconnect() {
    // kill any executing deployment actions
    synchronized (createdDeployments) {
      for (CancellableRunnable runnable : createdDeployments) {
        runnable.cancel();
      }
      createdDeployments.clear();
    }
  }

}
