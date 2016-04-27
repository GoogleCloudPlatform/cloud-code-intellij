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

import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.icons.AllIcons.General;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * Stops an App Engine based application running on GCP
 */
public class AppEngineStopAction extends AppEngineAction {
  private static final Logger logger = Logger.getInstance(AppEngineStopAction.class);

  private AppEngineHelper appEngineHelper;
  private UndeploymentTaskCallback callback;

  private Set<String> modulesToStop;
  private String versionToStop;

  public AppEngineStopAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull Set<String> modulesToStop,
      @NotNull String versionToStop,
      @NotNull UndeploymentTaskCallback callback) {
    super(loggingHandler, appEngineHelper, callback);

    this.appEngineHelper = appEngineHelper;
    this.modulesToStop = modulesToStop;
    this.versionToStop = versionToStop;
    this.callback = callback;
  }

  @Override
  public void run() {
    GeneralCommandLine commandLine = new GeneralCommandLine(
        appEngineHelper.getGcloudCommandPath().getAbsolutePath());

    commandLine.addParameters("preview", "app", "versions", "stop");
    commandLine.addParameters(versionToStop);

    commandLine.addParameter("--service");
    for(String module : modulesToStop) {
      commandLine.addParameter(module);
    }

    try {
      executeProcess(
          commandLine,
          new StopModuleProcessListener());
    } catch (ExecutionException e) {
      logger.warn(e);
      callback.errorOccurred(GctBundle.message("appengine.stop.modules.version.execution.error"));
    }
  }

  private class StopModuleProcessListener extends ProcessAdapter {

    @Override
    public void processTerminated(ProcessEvent event) {
      try {
        if (event.getExitCode() == 0) {
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
              "Application stop process exited with an error. Exit Code:" + event.getExitCode());
          callback.errorOccurred(
              GctBundle.message("appengine.stop.modules.version.execution.error.with.code",
                  event.getExitCode()));
        }
      } finally {
        deleteCredentials();
      }
    }

  }
}
