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

import com.google.cloud.tools.app.impl.cloudsdk.CloudSdkAppEngineVersions;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessOutputLineListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessStartListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.sdk.CloudSdk;
import com.google.cloud.tools.app.impl.config.DefaultVersionsSelectionConfiguration;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import javax.swing.SwingUtilities;

/**
 * Stops a module & version of an application running on App Engine.
 */
public class AppEngineStop {

  private static final Logger logger = Logger.getInstance(AppEngineStop.class);

  private AppEngineHelper helper;
  private LoggingHandler loggingHandler;
  private AppEngineDeploymentConfiguration configuration;
  private UndeploymentTaskCallback callback;

  /**
   * Initialize the stop dependencies.
   */
  public AppEngineStop(
      @NotNull AppEngineHelper helper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineDeploymentConfiguration configuration,
      @NotNull UndeploymentTaskCallback callback) {
    this.helper = helper;
    this.loggingHandler = loggingHandler;
    this.configuration = configuration;
    this.callback = callback;
  }

  /**
   * Stops the given module / version of an App Engine application.
   */
  public void stop(
      @NotNull String module,
      @NotNull String version,
      @NotNull ProcessStartListener startListener) {
    ProcessOutputLineListener outputListener = new ProcessOutputLineListener() {
      @Override
      public void outputLine(String line) {
        loggingHandler.print(line + "\n");
      }
    };

    ProcessExitListener stopExitListener = new StopExitListener();

    CloudSdk sdk = helper.createSdk(
        loggingHandler,
        startListener,
        outputListener,
        outputListener,
        stopExitListener);

    CloudSdkAppEngineVersions command = new CloudSdkAppEngineVersions(sdk);

    DefaultVersionsSelectionConfiguration configuration =
        new DefaultVersionsSelectionConfiguration();
    configuration.setVersions(Collections.singletonList(version));
    configuration.setService(module);

    command.stop(configuration);
  }

  AppEngineHelper getHelper() {
    return helper;
  }

  AppEngineDeploymentConfiguration getDeploymentConfiguration() {
    return configuration;
  }

  UndeploymentTaskCallback getCallback() {
    return callback;
  }

  private class StopExitListener implements ProcessExitListener {

    @Override
    public void exit(int exitCode) {
      try {
        if (exitCode == 0) {
          callback.succeeded();

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              Messages.showMessageDialog(
                  GctBundle.message("appengine.stop.modules.version.success.dialog.message"),
                  GctBundle.message("appengine.stop.modules.version.success.dialog.title"),
                  General.Information);
            }
          });
        } else {
          logger.warn(
              "Application stop process exited with an error. Exit Code:" + exitCode);
          callback.errorOccurred(
              GctBundle.message("appengine.stop.modules.version.execution.error.with.code",
                  exitCode));
        }
      } finally {
        helper.deleteCredentials();
      }
    }
  }
}
