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

import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;

import javax.swing.SwingUtilities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Takes care of undeploy and stop of App Engine applications.
 */
class AppEngineDeploymentRuntimeImpl extends DeploymentRuntime {

  private static final String STOP_CONFIRMATION_URI_OPEN_TAG =
      "<a href='https://cloud.google.com/appengine/docs/java/console/#versions'>";
  private static final String STOP_CONFIRMATION_URI_CLOSE_TAG = "</a>";

  private Project project;
  private AppEngineHelper appEngineHelper;
  private LoggingHandler loggingHandler;
  private DeployOutput deployOutput;

  public AppEngineDeploymentRuntimeImpl(
      @NotNull Project project,
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull String deploymentOutput) {
    this.project = project;
    this.appEngineHelper = appEngineHelper;
    this.loggingHandler = loggingHandler;

    try {
      deployOutput = parseDeployOutputToService(deploymentOutput);
    } catch (JsonParseException e) {
      loggingHandler.print(
          GctBundle.message("appengine.deployment.version.extraction.failure") + "\n");
      Logger.getInstance(AppEngineDeploymentRuntimeImpl.class).warn(
          "Could not retrieve service/version info of deployed application", e);
      deployOutput = null;
    }
  }

  // If failed to extract service/version info for the deployed app, undeploy will not be enabled.
  @Override
  public boolean isUndeploySupported() {
    return deployOutput != null;
  }

  @Override
  public void undeploy(@NotNull final UndeploymentTaskCallback callback) {
    if (deployOutput == null) {
      callback.errorOccurred(GctBundle.message("appengine.stop.modules.version.execution.error"));
      return;
    }

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
        loggingHandler,
        deployOutput.getService(),
        deployOutput.getVersion(),
        callback);

    ProgressManager.getInstance()
        .run(new Task.Backgroundable(project, "Stop App Engine", true,
            null) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            ApplicationManager.getApplication().invokeLater(appEngineStopAction);
          }
        });
  }

  @VisibleForTesting
  static DeployOutput parseDeployOutputToService(String jsonOutput) throws JsonParseException {
    /* An example JSON output of gcloud app deloy:
        {
          "configs": [],
          "versions": [
            {
              "id": "20160429t112518",
              "last_deployed_time": null,
              "project": "springboot-maven-project",
              "service": "default",
              "traffic_split": null,
              "version": null
            }
          ]
        }
    */
    Type deployOutputType = new TypeToken<DeployOutput>() {}.getType();
    DeployOutput deployOutput = new Gson().fromJson(jsonOutput, deployOutputType);
    if (deployOutput == null || deployOutput.versions == null ||
        deployOutput.versions.size() != 1) {
      throw new JsonParseException("Cannot get app version: unexpected gcloud JSON output format");
    }
    return deployOutput;
  }

  // Holds de-serialized JSON output of gcloud app deploy. Don't change the field names
  // because Gson uses it for automatic de-serialization.
  @VisibleForTesting
  @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Initialized by Gson")
  static class DeployOutput {
    private static class Version {
      String id;
      String service;
    }
    List<Version> versions;

    @NotNull
    public String getVersion() {
      if (versions == null || versions.size() != 1 || versions.get(0).id == null) {
        throw new AssertionError("Should be called only when 'versions' have one element.");
      }
      return versions.get(0).id;
    }

    @NotNull
    public String getService() {
      if (versions == null || versions.size() != 1 || versions.get(0).service == null) {
        throw new AssertionError("Should be called only when 'versions' have one element.");
      }
      return versions.get(0).service;
    }
  }
}