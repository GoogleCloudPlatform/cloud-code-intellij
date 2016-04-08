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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine.ParentEnvironmentType;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.remoteServer.runtime.RemoteOperationCallback;
import com.intellij.remoteServer.runtime.log.LoggingHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Base class for App Engine runnable actions - e.g. deploy, stop. Provides implementations
 * for executing CLI based commands.
 */
public abstract class AppEngineAction implements Runnable {
  private static final Logger logger = Logger.getInstance(AppEngineAction.class);

  private LoggingHandler loggingHandler;
  private AppEngineHelper appEngineHelper;
  private File credentialsPath;
  private RemoteOperationCallback callback;
  private ProcessHandler processHandler;

  public AppEngineAction(
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineHelper appEngineHelper,
      @NotNull RemoteOperationCallback callback) {
    this.loggingHandler = loggingHandler;
    this.appEngineHelper = appEngineHelper;
    this.callback = callback;
  }

  /**
   * Returns the commandline process handler used to execute the action
   *
   * @return the process handler, or null, if the action is not executing
   */
  public ProcessHandler getProcessHandler() {
    return processHandler;
  }

  protected LoggingHandler getLoggingHandler() {
    return loggingHandler;
  }

  protected void executeProcess(
      @NotNull GeneralCommandLine commandLine,
      @NotNull ProcessListener listener) throws ExecutionException {

    // kill action process if one is already executing
    if (processHandler != null) {
      processHandler.destroyProcess();
      processHandler = null;
    }

    credentialsPath = createApplicationDefaultCredentials();
    if (credentialsPath == null) {
      callback.errorOccurred(
          GctBundle.message("appengine.deployment.credential.not.found",
              appEngineHelper.getGoogleUsername()));
      return;
    }

    // Common command line settings
    commandLine.addParameter("--project=" + appEngineHelper.getProjectId());
    commandLine
        .addParameter("--credential-file-override=" + credentialsPath.getAbsolutePath());
    commandLine.withParentEnvironmentType(ParentEnvironmentType.CONSOLE);
    commandLine.getEnvironment().put("CLOUDSDK_METRICS_ENVIRONMENT", "gcloud-intellij");
    commandLine.getEnvironment().put("CLOUDSDK_APP_USE_GSUTIL", "0");

    consoleLogLn("Executing: " + commandLine.getCommandLineString());

    Process process = commandLine.createProcess();
    processHandler = new OSProcessHandler(process,
        commandLine.getCommandLineString());
    loggingHandler.attachToProcess(processHandler);
    processHandler.addProcessListener(listener);

    // mark process as completed by setting handler to null when terminated
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        processHandler = null;
      }
    });

    processHandler.startNotify();
  }

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";

  @VisibleForTesting
  @Nullable
  protected File createApplicationDefaultCredentials() {
    CredentialedUser projectUser = Services.getLoginService().getAllUsers()
        .get(appEngineHelper.getGoogleUsername());

    GoogleLoginState googleLoginState;
    if (projectUser != null) {
      googleLoginState = projectUser.getGoogleLoginState();
    } else {
      return null;
    }
    String clientId = googleLoginState.fetchOAuth2ClientId();
    String clientSecret = googleLoginState.fetchOAuth2ClientSecret();
    String refreshToken = googleLoginState.fetchOAuth2RefreshToken();
    Map<String, String> credentialMap = ImmutableMap.of(
        CLIENT_ID_LABEL, clientId,
        CLIENT_SECRET_LABEL, clientSecret,
        REFRESH_TOKEN_LABEL, refreshToken,
        GCLOUD_USER_TYPE_LABEL, GCLOUD_USER_TYPE
    );
    String jsonCredential = new Gson().toJson(credentialMap);
    File tempCredentialFilePath = null;
    try {
      tempCredentialFilePath = FileUtil
          .createTempFile(
              "tmp_google_application_default_credential",
              "json",
              true /* deleteOnExit */);
      Files.write(jsonCredential, tempCredentialFilePath, Charset.forName("UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return tempCredentialFilePath;
  }

  protected void deleteCredentials() {
    if (credentialsPath != null && credentialsPath.exists()) {
      if (!credentialsPath.delete()) {
        logger.warn("failed to delete credential file expected at "
            + credentialsPath.getPath());
      }
    }
  }

  protected void consoleLogLn(String message,
      String... arguments) {
    loggingHandler.print(String.format(message + "\n", (Object[]) arguments));
  }
}

