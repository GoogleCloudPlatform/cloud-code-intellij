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

import com.google.cloud.tools.intellij.appengine.action.AppEngineAction;
import com.google.cloud.tools.intellij.appengine.action.AppEngineHelper;
import com.google.cloud.tools.intellij.appengine.action.configuration.AppEngineDeploymentConfiguration;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.SwingUtilities;

/**
 * Takes care of undeploy and stop of App Engine applications.
 */
public class AppEngineDeploymentRuntime extends DeploymentRuntime {

  private static final String STOP_CONFIRMATION_URI_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/docs/java/console/#versions'>";
  private static final String STOP_CONFIRMATION_URI_CLOSE_TAG = "</a>";

  private Project project;
  private AppEngineHelper appEngineHelper;
  private LoggingHandler loggingHandler;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private String service;
  private String version;

  /**
   * Initialize the deployment runtime.
   */
  public AppEngineDeploymentRuntime(
      @NotNull Project project,
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration,
      @Nullable String service,
      @Nullable String version) {
    this.project = project;
    this.appEngineHelper = appEngineHelper;
    this.loggingHandler = loggingHandler;
    this.deploymentConfiguration = deploymentConfiguration;
    this.service = service;
    this.version = version;
  }

  @Override
  public boolean isUndeploySupported() {
    return service != null && version != null;
  }

  @Override
  public void undeploy(@NotNull final UndeploymentTaskCallback callback) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        int doStop = Messages
            .showOkCancelDialog(
                GctBundle.message(
                    "appengine.stop.modules.version.confirmation.message",
                    STOP_CONFIRMATION_URI_OPEN_TAG,
                    STOP_CONFIRMATION_URI_CLOSE_TAG),
                GctBundle.message("appengine.stop.modules.version.confirmation.title"),
                General.Warning);

        if (doStop == Messages.YES) {
          stop(callback);
        } else {
          callback.errorOccurred(
              GctBundle.message("appengine.stop.modules.version.canceled.message"));
        }
      }
    });
  }

  private void stop(@NotNull UndeploymentTaskCallback callback) {
    final AppEngineAction appEngineStopAction = appEngineHelper.createStopAction(
        loggingHandler, deploymentConfiguration, service, version, callback);

    ProgressManager.getInstance()
        .run(new Task.Backgroundable(project, "Stop App Engine", true,
            null) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            ApplicationManager.getApplication().invokeLater(appEngineStopAction);
          }
        });
  }
}