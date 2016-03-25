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

package com.google.gct.idea.appengine.cloud;

import com.google.gct.idea.util.GctBundle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.remoteServer.runtime.deployment.DeploymentRuntime.UndeploymentTaskCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Stops a ManagedVM based application running on GCP.
 */
public class ManagedVmStopAction extends ManagedVmAction {
  private static final Logger logger = Logger.getInstance(ManagedVmStopAction.class);

  private AppEngineHelper appEngineHelper;
  private UndeploymentTaskCallback callback;

  private boolean moduleListingStarted;

  public ManagedVmStopAction(
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull LoggingHandler loggingHandler,
      @NotNull UndeploymentTaskCallback callback) {
    super(loggingHandler, appEngineHelper);

    this.appEngineHelper = appEngineHelper;
    this.callback = callback;
  }

  @Override
  public void run() {
    final File appDefaultCredentialsPath = createApplicationDefaultCredentials();
    if (appDefaultCredentialsPath == null) {
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.credential.not.found",
              appEngineHelper.getGoogleUsername()));
      return;
    }
    GeneralCommandLine commandLine = new GeneralCommandLine(
        appEngineHelper.getGcloudCommandPath().getAbsolutePath());

    commandLine.addParameters("preview", "app", "modules", "list");

    commandLine.addParameter("--project=" + appEngineHelper.getProjectId());
    commandLine
        .addParameter("--credential-file-override=" + appDefaultCredentialsPath.getAbsolutePath());
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);

    try {
      executeProcess(commandLine, new ListModulesProcessListener(appDefaultCredentialsPath));
    } catch (ExecutionException e) {
      logger.error(e);
      callback.errorOccurred(GctBundle.message("appengine.stopapp.error.during.execution"));
    }
  }

  private class ListModulesProcessListener implements ProcessListener {
    private File defaultCredentialsPath;
    private AppEngineModuleListOutputParser outputParser = new AppEngineModuleListOutputParser();

    public ListModulesProcessListener(File defaultCredentialsPath) {
      this.defaultCredentialsPath = defaultCredentialsPath;
    }

    @Override
    public void startNotified(ProcessEvent event) {
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      if (event.getExitCode() == 0) {
        callback.succeeded();
      } else {
        stopApplicationFailure(event);
        deleteCredentials(defaultCredentialsPath);
      }
    }

    @Override
    public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {
      AppEngineModuleListItem item = outputParser.getLatestDeployedModule();

      if (item != null && item.getTrafficSplit() > 0) {
        GeneralCommandLine commandLine = new GeneralCommandLine(
            appEngineHelper.getGcloudCommandPath().getAbsolutePath());

        commandLine.addParameters("preview", "app", "modules", "stop");
        commandLine.addParameter(item.getModuleName());
        commandLine.addParameters("--version", item.getVersion());

        commandLine.addParameter("--project=" + appEngineHelper.getProjectId());
        commandLine
            .addParameter("--credential-file-override=" + defaultCredentialsPath.getAbsolutePath());
        commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);

        try {
          executeProcess(commandLine, new StopModuleProcessListener(defaultCredentialsPath));
        } catch (ExecutionException e) {
          logger.error(e);
          callback.errorOccurred(GctBundle.message("appengine.stopapp.error.during.execution"));
        }
      } else {
        logger.error("Failed to stop running application because no instances with traffic "
            + "allocation were found.");
        callback.errorOccurred(GctBundle.message("appengine.stopapp.error.during.execution"));
      }
    }

    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {
      if (outputType.equals(ProcessOutputTypes.STDOUT)) {
        if (event.getText().toLowerCase().startsWith("module")) {
          moduleListingStarted = true;
        } else if (moduleListingStarted) {
          outputParser.addLineItem(event.getText());
        }
      }
    }
  }

  private class StopModuleProcessListener extends ProcessAdapter {
    private File defaultCredentialsPath;

    public StopModuleProcessListener(File defaultCredentialsPath) {
      this.defaultCredentialsPath = defaultCredentialsPath;
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      if (event.getExitCode() == 0) {
        callback.succeeded();
      } else {
        stopApplicationFailure(event);
      }

      deleteCredentials(defaultCredentialsPath);
    }
  }

  private void stopApplicationFailure(ProcessEvent event) {
    logger.error("Application stop process exited with an error. Exit Code:" + event.getExitCode());
    callback.errorOccurred(
        GctBundle.message("appengine.stopapp.error.with.code", event.getExitCode()));
  }
}
