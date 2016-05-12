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
import com.google.cloud.tools.app.impl.cloudsdk.internal.process.DefaultProcessRunner;
import com.google.cloud.tools.app.impl.cloudsdk.internal.sdk.CloudSdk;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gson.Gson;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
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
  private DefaultProcessRunner processRunner;
  boolean cancelled = false;

  public AppEngineAction(
      @NotNull LoggingHandler loggingHandler,
      @NotNull AppEngineHelper appEngineHelper) {
    this.loggingHandler = loggingHandler;
    this.appEngineHelper = appEngineHelper;
  }

  LoggingHandler getLoggingHandler() {
    return loggingHandler;
  }

  /**
   * Creates and stages credential file used for executing cloud sdk actions and returns
   * {@link CloudSdk} instance.
   *
   * @param processRunner a {@link DefaultProcessRunner} for managing the process that runs the
   *   action.
   */
  @NotNull
  CloudSdk prepareExecution(@NotNull DefaultProcessRunner processRunner) throws AppEngineException {
    this.processRunner = processRunner;

    credentialsPath = createApplicationDefaultCredentials();
    if (credentialsPath == null) {
      consoleLogLn(GctBundle.message("appengine.action.credential.not.found",
          appEngineHelper.getGoogleUsername()));
      throw new AppEngineException("Failed to create application default credentials.");
    }

    return new CloudSdk.Builder()
        .sdkPath(appEngineHelper.getGcloudCommandPath())
        .processRunner(processRunner)
        .appCommandCredentialFile(credentialsPath)
        .appCommandMetricsEnvironment("gcloud-intellij")
        .appCommandGsUtil(1)
        .appCommandOutputFormat("json")
        .build();
  }

  /**
   * Kill any executing process for the action.
   */
  void cancel() {
    if (processRunner != null
        && processRunner.getProcess() != null) {
      cancelled = true;
      processRunner.getProcess().destroy();
    }
  }

  private static final String CLIENT_ID_LABEL = "client_id";
  private static final String CLIENT_SECRET_LABEL = "client_secret";
  private static final String REFRESH_TOKEN_LABEL = "refresh_token";
  private static final String GCLOUD_USER_TYPE_LABEL = "type";
  private static final String GCLOUD_USER_TYPE = "authorized_user";

  /**
   * Create and stage a temporary credentials file used by various cloud sdk actions.
   */
  @VisibleForTesting
  @Nullable
  File createApplicationDefaultCredentials() {
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
    File tempCredentialFilePath;
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

  /**
   * Delete the credential file if it exists.
   */
  void deleteCredentials() {
    if (credentialsPath != null && credentialsPath.exists()) {
      if (!credentialsPath.delete()) {
        logger.warn("failed to delete credential file expected at "
            + credentialsPath.getPath());
      }
    }
  }

  void consoleLogLn(String message,
      String... arguments) {
    loggingHandler.print(String.format(message + "\n", (Object[]) arguments));
  }
}

