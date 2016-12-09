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

package com.google.cloud.tools.intellij.debugger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Builder;
import com.google.api.services.clouddebugger.v2.Clouddebugger.Debugger;
import com.google.cloud.tools.intellij.CloudToolsPluginInfoService;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdt.eclipse.login.common.LoginListener;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to return clients on a per user email basis.
 */
public class CloudDebuggerClient {

  @VisibleForTesting
  static final int LONG_CONNECTION_TIMEOUT_MS = 120 * 1000;
  @VisibleForTesting
  static final int SHORT_CONNECTION_TIMEOUT_MS = 10 * 1000;

  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final Logger LOG = Logger.getInstance(CloudDebuggerClient.class);
  private static final String ROOT_URL = "https://clouddebugger.googleapis.com";
  private static final ConcurrentHashMap<String, Debugger> debuggerClientsFromUserEmail =
      new ConcurrentHashMap<String, Debugger>();

  private CloudDebuggerClient() {
  }

  /**
   * Returns a cloud debugger connection given {@link CloudDebugProcessState} to indicate the
   * credentials to use. The function may return null if the user is not logged in. TODO: Create a
   * better experience attaching when not logged in TODO: Handle cases where the user logs out in
   * the middle of a debug session.
   */
  @Nullable
  public static Debugger getLongTimeoutClient(final @NotNull CloudDebugProcessState state) {
    return getClient(state.getUserEmail(), LONG_CONNECTION_TIMEOUT_MS);
  }

  public static Debugger getLongTimeoutClient(final @Nullable String userEmail) {
    return getClient(userEmail, LONG_CONNECTION_TIMEOUT_MS);
  }

  public static Debugger getShortTimeoutClient(final @NotNull CloudDebugProcessState state) {
    return getClient(state.getUserEmail(), SHORT_CONNECTION_TIMEOUT_MS);
  }

  public static Debugger getShortTimeoutClient(final @Nullable String userEmail) {
    return getClient(userEmail, SHORT_CONNECTION_TIMEOUT_MS);
  }

  /**
   * Returns a cloud debugger connection given a user email to indicate the credentials to use. The
   * function may return null if the user is not logged in.
   */
  @Nullable
  private static Debugger getClient(final @Nullable String userEmail, final int timeout) {
    if (Strings.isNullOrEmpty(userEmail)) {
      LOG.warn("unexpected null email in controller initialize.");
      return null;
    }
    final String hashkey = userEmail + timeout;
    Debugger cloudDebuggerClient = debuggerClientsFromUserEmail.get(hashkey);
    if (cloudDebuggerClient == null) {
      try {
        final CredentialedUser user = Services.getLoginService().getAllUsers().get(userEmail);
        final Credential credential = (user != null ? user.getCredential() : null);
        if (credential != null) {
          user.getGoogleLoginState().addLoginListener(new LoginListener() {
            @Override
            public void statusChanged(boolean login) {
              if (!login) {
                // aggressively remove the cached item on any status change.
                debuggerClientsFromUserEmail.remove(hashkey);
              } else { // NOPMD
                // user logged in, should we do something?
              }
            }
          });
          HttpRequestInitializer initializer = new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
              HttpHeaders headers = new HttpHeaders();
              httpRequest.setConnectTimeout(timeout);
              httpRequest.setReadTimeout(timeout);
              httpRequest.setHeaders(headers);
              credential.initialize(httpRequest);
            }
          };

          HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
          String userAgent = ServiceManager
              .getService(CloudToolsPluginInfoService.class).getUserAgent();
          cloudDebuggerClient = new Builder(httpTransport, JSON_FACTORY, initializer)
              .setRootUrl(ROOT_URL)
              // this ends up prefixed to user agent
              .setApplicationName(userAgent)
              .build().debugger();
        }
      } catch (IOException ex) {
        LOG.warn("Error connecting to Cloud Debugger API", ex);
      } catch (GeneralSecurityException ex) {
        LOG.warn("Error connecting to Cloud Debugger API", ex);
      }

      if (cloudDebuggerClient != null) {
        debuggerClientsFromUserEmail.put(hashkey, cloudDebuggerClient);
      }
    }

    return cloudDebuggerClient;
  }

  @TestOnly
  static void setClient(@NotNull String userEmail, @NotNull Debugger mockClient) {
    debuggerClientsFromUserEmail.put(userEmail, mockClient);
  }
}
