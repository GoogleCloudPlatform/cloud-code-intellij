/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.debugger.Debugger;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.GoogleLoginUtils;
import com.google.gdt.eclipse.login.common.LoginListener;
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
  private static final int CONNECTION_TIMEOUT_MS = 120 * 1000;
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final Logger LOG = Logger.getInstance(CloudDebuggerClient.class);
  private static final String ROOT_URL = "https://www.googleapis.com";
  private static final ConcurrentHashMap<String, Debugger> myDebuggerClientsFromUserEmail =
    new ConcurrentHashMap<String, Debugger>();

  private CloudDebuggerClient() {
  }

  /**
   * Returns a cloud debugger connection given {@link CloudDebugProcessState} to indicate the credentials to use. The
   * function may return null if the user is not logged in.
   * TODO: Create a better experience attaching when not logged in
   * TODO: Handle cases where the user logs out in the middle of a debug session.
   */
  @Nullable
  public static Debugger getCloudDebuggerClient(final @NotNull CloudDebugProcessState state) {
    return getCloudDebuggerClient(state.getUserEmail());
  }

  /**
   * Returns a cloud debugger connection given a user email to indicate the credentials to use. The function may return
   * null if the user is not logged in.
   */
  @Nullable
  public static Debugger getCloudDebuggerClient(final @Nullable String userEmail) {
    if (Strings.isNullOrEmpty(userEmail)) {
      LOG.warn("unexpected null email in controller initialize.");
      return null;
    }
    Debugger cloudDebuggerClient = myDebuggerClientsFromUserEmail.get(userEmail);

    if (cloudDebuggerClient == null) {
      try {
        final CredentialedUser user = GoogleLogin.getInstance().getAllUsers().get(userEmail);
        final Credential credential = (user != null ? user.getCredential() : null);
        if (credential != null) {
          user.getGoogleLoginState().addLoginListener(new LoginListener() {
            @Override
            public void statusChanged(boolean login) {
              //aggresively remove the cached item on any status change.
              myDebuggerClientsFromUserEmail.remove(userEmail);
            }
          });
          HttpRequestInitializer initializer = new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
              httpRequest.setConnectTimeout(CONNECTION_TIMEOUT_MS);
              httpRequest.setReadTimeout(CONNECTION_TIMEOUT_MS);
              credential.initialize(httpRequest);
            }
          };

          HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
          cloudDebuggerClient = new Debugger.Builder(httpTransport, JSON_FACTORY, initializer).setRootUrl(ROOT_URL)
            .setApplicationName(GoogleLoginUtils.getCurrentPlatformName()).build();
        }
      }
      catch (IOException ex) {
        LOG.warn("Error connecting to Cloud Debugger API", ex);
      }
      catch (GeneralSecurityException ex) {
        LOG.warn("Error connecting to Cloud Debugger API", ex);
      }

      if (cloudDebuggerClient != null) {
        myDebuggerClientsFromUserEmail.put(userEmail, cloudDebuggerClient);
      }
    }
    return cloudDebuggerClient;
  }

  @TestOnly
  static void setClient(@NotNull String userEmail, @NotNull Debugger mockClient) {
    myDebuggerClientsFromUserEmail.put(userEmail, mockClient);
  }
}
