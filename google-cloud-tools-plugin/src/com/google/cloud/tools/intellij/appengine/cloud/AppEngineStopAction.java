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

import com.google.cloud.tools.app.api.AppEngineException;
import com.google.cloud.tools.app.impl.cloudsdk.CloudSdkAppEngineVersions;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.DefaultProcessRunner;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessExitListener;
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.ProcessOutputLineListener;
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
 * Stops an App Engine based application running on GCP
 */
public class AppEngineStopAction extends AppEngineAction {
  private static final Logger logger = Logger.getInstance(AppEngineStopAction.class);

  private UndeploymentTaskCallback callback;

  private String moduleToStop;
  private String versionToStop;

  public AppEngineStopAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull String moduleToStop,
      @NotNull String versionToStop,
      @NotNull UndeploymentTaskCallback callback) {
    super(loggingHandler, appEngineHelper, callback);

    this.moduleToStop = moduleToStop;
    this.versionToStop = versionToStop;
    this.callback = callback;
  }

  @Override
  public void run() {
    CloudSdk sdk;

    try {
      sdk = prepareExecution(createStopProcessRunner());
    } catch(AppEngineException ex) {
      callback.errorOccurred(GctBundle.message("appengine.stop.modules.version.error"));
      return;
    }

    CloudSdkAppEngineVersions command = new CloudSdkAppEngineVersions(sdk);

    DefaultVersionsSelectionConfiguration configuration =
        new DefaultVersionsSelectionConfiguration();
    configuration.setVersions(Collections.singletonList(versionToStop));
    configuration.setService(moduleToStop);

    command.stop(configuration);
  }

  private DefaultProcessRunner createStopProcessRunner() {
    DefaultProcessRunner processRunner =
        new DefaultProcessRunner(new ProcessBuilder());
    processRunner.setAsync(true);

    ProcessOutputLineListener lineListener = new ProcessOutputLineListener() {
      @Override
      public void outputLine(String output) {
        consoleLogLn(output);
      }
    };

    processRunner.setStdErrLineListener(lineListener);
    processRunner.setStdOutLineListener(lineListener);
    processRunner.setExitListener(new StopExitListener());

    return processRunner;
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
        deleteCredentials();
      }
    }
  }
}
