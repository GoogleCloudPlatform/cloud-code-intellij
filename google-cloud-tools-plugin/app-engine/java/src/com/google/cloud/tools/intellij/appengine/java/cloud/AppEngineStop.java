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

package com.google.cloud.tools.intellij.appengine.java.cloud;

import com.google.cloud.tools.appengine.api.versions.DefaultVersionsSelectionConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdkAppEngineVersions;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessExitListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessStartListener;
import com.google.cloud.tools.intellij.appengine.java.AppEngineMessageBundle;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;
import java.util.Collections;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;

/** Stops a module & version of an application running on App Engine. */
public class AppEngineStop {

  private static final Logger logger = Logger.getInstance(AppEngineStop.class);

  private AppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private AppEngineDeploymentConfiguration deploymentConfiguration;
  private UndeploymentTaskCallback callback;

  /** Initialize the stop dependencies. */
  public AppEngineStop(
      @NotNull AppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineDeploymentConfiguration deploymentConfiguration,
      @NotNull UndeploymentTaskCallback callback) {
    this.helper = helper;
    this.loggingHandler = loggingHandler;
    this.deploymentConfiguration = deploymentConfiguration;
    this.callback = callback;
  }

  /** Stops the given module / version of an App Engine application. */
  public void stop(
      @NotNull String module,
      @NotNull String version,
      @NotNull ProcessStartListener startListener) {
    ProcessOutputLineListener outputListener =
        new ProcessOutputLineListener() {
          @Override
          public void onOutputLine(String line) {
            loggingHandler.print(line + "\n");
          }
        };

    ProcessExitListener stopExitListener = new StopExitListener();

    CloudSdk sdk =
        helper.createSdk(
            loggingHandler, startListener, outputListener, outputListener, stopExitListener);

    DefaultVersionsSelectionConfiguration configuration =
        new DefaultVersionsSelectionConfiguration();
    configuration.setVersions(Collections.singletonList(version));
    configuration.setService(module);
    configuration.setProject(deploymentConfiguration.getCloudProjectName());

    CloudSdkAppEngineVersions command = new CloudSdkAppEngineVersions(sdk);
    command.stop(configuration);
  }

  public AppEngineHelper getHelper() {
    return helper;
  }

  public AppEngineDeploymentConfiguration getDeploymentConfiguration() {
    return deploymentConfiguration;
  }

  public UndeploymentTaskCallback getCallback() {
    return callback;
  }

  private class StopExitListener implements ProcessExitListener {

    @Override
    public void onExit(int exitCode) {
      try {
        if (exitCode == 0) {
          callback.succeeded();

          SwingUtilities.invokeLater(
              new Runnable() {
                @Override
                public void run() {
                  Messages.showMessageDialog(
                      AppEngineMessageBundle.message(
                          "appengine.stop.modules.version.success.dialog.message"),
                      AppEngineMessageBundle.message(
                          "appengine.stop.modules.version.success.dialog.title"),
                      General.Information);
                }
              });
        } else {
          logger.warn("Application stop process exited with an error. Exit Code:" + exitCode);
          callback.errorOccurred(
              AppEngineMessageBundle.message(
                  "appengine.stop.modules.version.execution.error.with.code", exitCode));
        }
      } finally {
        helper.deleteCredentials();
      }
    }
  }
}
