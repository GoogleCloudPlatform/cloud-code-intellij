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

package com.google.cloud.tools.intellij.vcs;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.resources.SelectUserDialog;
import com.google.cloud.tools.intellij.util.GctBundle;

import com.intellij.ide.DataManager;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.AuthData;
import com.intellij.util.UriUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Window;
import java.io.IOException;

import git4idea.DialogManager;
import git4idea.commands.GitHttpAuthenticator;
import git4idea.remote.GitHttpAuthDataProvider;
import git4idea.remote.GitRememberedInputs;

/**
 * Provides credential information for URLs pointing to Google's cloud source.
 */
public class GcpHttpAuthDataProvider implements GitHttpAuthDataProvider {

  private static final Logger LOG = Logger.getInstance(GcpHttpAuthDataProvider.class);

  public static final String GOOGLE_URL = "https://source.developers.google.com";
  public static final String GCP_USER = "com.google.gct.idea.git.username";
  private static final String GOOGLE_URL_ALT = "http://source.developers.google.com";

  private String selectedUser;
  private boolean chooseManualLogin;
  private static Project currentProject;

  @Nullable
  @Override
  public AuthData getAuthData(@NotNull String url) {
    final Project currentProject = getCurrentProject();
    if ((currentProject != null || Context.currentContext != null) && isGcpUrl(url)) {
      Context currentContext = Context.currentContext; //always prefer context over project setting.
      String userEmail = currentContext != null ? currentContext.userName : null;

      if (Strings.isNullOrEmpty(userEmail) && currentProject != null) {
        userEmail = PropertiesComponent.getInstance(currentProject).getValue(GCP_USER, "");
      }
      CredentialedUser targetUser = getUserFromEmail(userEmail);

      if (targetUser == null) {
        //show a dialog allowing the user to select a login.  (new project recognized)
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          @Override
          public void run() {
            SelectUserDialog dialog = new SelectUserDialog(currentProject,
                GctBundle.getString("httpauthprovider.chooselogin"));
            DialogManager.show(dialog);
            chooseManualLogin = !dialog.isOK();
            selectedUser = dialog.getSelectedUser();
          }
        }, ModalityState.defaultModalityState());

        if (chooseManualLogin) {
          return null;
        }
        userEmail = selectedUser;
        targetUser = getUserFromEmail(userEmail);
        if (targetUser != null && currentProject != null && Context.currentContext == null) {
          PropertiesComponent.getInstance(currentProject).setValue(GCP_USER, userEmail);
        }
      }

      if (targetUser != null) {
        try {
          return new AuthData(targetUser.getEmail(),
              targetUser.getGoogleLoginState().fetchAccessToken());
        } catch (IOException ex) {
          LOG.error("IOException creating authdata:" + ex.toString());
        }
      }

    }
    return null;
  }

  /**
   * Clears out IDE managed Cloud Repository credentials.
   *
   * This is needed to force the Git integration to use the {@link GcpHttpAuthDataProvider}. There
   * is a default auth provider that is created by the integration that will override this one if
   * there exist IDE managed credentials (i.e. retrievable via {@link PasswordSafe}).
   *
   * The
   */
  public static void clearIdeStoredGcpCredentials() {
    PasswordSafe passwordSafe = PasswordSafe.getInstance();
    String user = GitRememberedInputs.getInstance()
        .getUserNameForUrl(GcpHttpAuthDataProvider.GOOGLE_URL_ALT);
    String key = makeKey(GcpHttpAuthDataProvider.GOOGLE_URL_ALT, user);

    passwordSafe.setPassword(GitHttpAuthenticator.class, key == null ? "" : key, null);
  }

  /**
   * Check if url is a Google Cloud Platform URL.
   */
  public static boolean isGcpUrl(@Nullable String url) {
    return (url != null
        && (StringUtil.startsWithIgnoreCase(url, GOOGLE_URL) || StringUtil
            .startsWithIgnoreCase(url, GOOGLE_URL_ALT)));
  }

  public static String getGcpUrl(String projectId, String repositoryId) {
    return GOOGLE_URL + "/p/" + projectId + "/r/" + repositoryId + "/";
  }

  @NotNull
  public static Context createContext(@Nullable String userName) {
    return Context.create(userName);
  }

  @Nullable
  private static CredentialedUser getUserFromEmail(@Nullable String email) {
    if (Strings.isNullOrEmpty(email)) {
      return null;
    }

    for (CredentialedUser user : Services.getLoginService().getAllUsers().values()) {
      if (email != null && email.equalsIgnoreCase(user.getEmail())) {
        return user;
      }
    }

    return null;
  }

  @Override
  public void forgetPassword(@NotNull String url) {
    Project currentProject = getCurrentProject();

    if (currentProject != null) {
      PropertiesComponent.getInstance(currentProject).unsetValue(GCP_USER);
    }
  }

  @Nullable
  private static Project getCurrentProject() {
    Project result = null;
    Window activeWindow = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
    if (activeWindow != null) {
      result = CommonDataKeys.PROJECT
          .getData(DataManager.getInstance().getDataContext(activeWindow));
    }
    return result != null ? result : currentProject;
  }

  /**
   * Creates a String used a key for the password store in the form of:
   * http[s]://{login}@{url}
   */
  private static String makeKey(@NotNull String url, @Nullable String login) {
    if (login == null) {
      return url;
    } else {
      Couple pair = UriUtil.splitScheme(url);
      String scheme = (String) pair.getFirst();

      return !StringUtil.isEmpty(scheme)
          ? scheme + "://" + login + "@" + pair.getSecond()
          : login + "@" + url;
    }
  }

  /**
   * This class allows IJ to perform Git operations without a project, but with context that will
   * give the authdataprovider enough information to log in.  This is used in the checkout from GCP
   * scenario, where the user has explicitly chosen a GCP project (with username), but there isn't a
   * project yet to choose from.
   */
  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  public static class Context {

    private static Context currentContext = null;
    private String userName;

    private Context(@Nullable String userName) {
      this.userName = userName;
    }

    /**
     * Creates a new context.
     */
    public static Context create(@Nullable String userName) {
      Context newContext = new Context(userName);
      assert currentContext == null;
      currentContext = newContext;
      return newContext;
    }

    /**
     * Close the current context.
     */
    public void close() {
      if (currentContext == this) {
        currentContext = null;
      }
    }
  }
}
