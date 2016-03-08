/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.google.gct.idea.git;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.annotations.VisibleForTesting;
import com.google.gct.idea.elysium.SelectUserDialog;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.AuthData;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import git4idea.DialogManager;
import git4idea.remote.GitHttpAuthDataProvider;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Window;
import java.io.IOException;

/**
 * Provides credential information for URLs pointing to Google's cloud source.
 */
public class GcpHttpAuthDataProvider implements GitHttpAuthDataProvider {
  private static final Logger LOG = Logger.getInstance(GcpHttpAuthDataProvider.class);

  public static final String GOOGLE_URL = "https://source.developers.google.com";
  public static final String GCP_USER = "com.google.gct.idea.git.username";
  private static final String GOOGLE_URL_ALT = "http://source.developers.google.com";

  private String mySelectedUser;
  private boolean myChooseManualLogin;
  private static Project ourCurrentProject;

  @Nullable
  @Override
  @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
  public AuthData getAuthData(@NotNull String url) {
    final Project currentProject = getCurrentProject();
    if ((currentProject != null || Context.ourCurrentContext != null) && isUrlGCP(url)) {
      Context currentContext = Context.ourCurrentContext; //always prefer context over project setting.
      String userEmail = currentContext != null ? currentContext.myUserName : null;

      if (Strings.isNullOrEmpty(userEmail) && currentProject != null) {
        userEmail = PropertiesComponent.getInstance(currentProject).getValue(GCP_USER, "");
      }
      CredentialedUser targetUser = getUserFromEmail(userEmail);

      if (targetUser == null) {
        //show a dialog allowing the user to select a login.  (new project recognized)
        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
          @Override
          public void run() {
            SelectUserDialog dialog = new SelectUserDialog(currentProject, GctBundle.getString("httpauthprovider.chooselogin"));
            DialogManager.show(dialog);
            myChooseManualLogin = !dialog.isOK();
            mySelectedUser = dialog.getSelectedUser();
          }
        }, ModalityState.defaultModalityState());

        if (myChooseManualLogin) {
          return null;
        }
        userEmail = mySelectedUser;
        targetUser = getUserFromEmail(userEmail);
        if (targetUser != null && currentProject != null && Context.ourCurrentContext == null) {
          PropertiesComponent.getInstance(currentProject).setValue(GCP_USER, userEmail);
        }
      }

      if (targetUser != null) {
        try {
          return new AuthData(targetUser.getEmail(), targetUser.getGoogleLoginState().fetchAccessToken());
        }
        catch (IOException ex) {
          LOG.error("IOException creating authdata:" + ex.toString());
        }
      }

    }
    return null;
  }

  public static boolean isUrlGCP(@Nullable String url) {
    return (url != null &&
      (StringUtil.startsWithIgnoreCase(url, GOOGLE_URL) || StringUtil.startsWithIgnoreCase(url, GOOGLE_URL_ALT)));
  }

  public static String getGcpUrl(String projectId) {
    return "https://source.developers.google.com/p/" + projectId + "/";
  }

  @Nullable
  public static String findGCPRemoteUrl(@NotNull GitRepository repository) {
    Pair<GitRemote, String> remote = findGCPRemote(repository);
    if (remote == null) {
      return null;
    }
    return remote.getSecond();
  }

  @Nullable
  public static Pair<GitRemote, String> findGCPRemote(@NotNull GitRepository repository) {
    for (GitRemote gitRemote : repository.getRemotes()) {
      for (String remoteUrl : gitRemote.getUrls()) {
        if (isUrlGCP(remoteUrl)) {
          return Pair.create(gitRemote, remoteUrl);
        }
      }
    }
    return null;
  }

  @NotNull
  public static Context createContext(@Nullable String userName) {
    return Context.Create(userName);
  }

  @Nullable
  private static CredentialedUser getUserFromEmail(@Nullable String email) {
    if (Strings.isNullOrEmpty(email)) {
      return null;
    }

    for (CredentialedUser user : GoogleLogin.getInstance().getAllUsers().values()) {
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
      result = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(activeWindow));
    }
    return result != null ? result : ourCurrentProject;
  }

  @VisibleForTesting
  static void setCurrentProject(Project project) {
    ourCurrentProject = project;
  }

  /**
   * This class allows IJ to perform Git operations without a project, but with context that will give the authdataprovider
   * enough information to log in.  This is used in the checkout from GCP scenario, where the user has explicitly chosen
   * a GCP project (with username), but there isn't a project yet to choose from.
   */
  @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
  public static class Context {
    private static Context ourCurrentContext = null;
    private String myUserName;

    private Context(@Nullable String userName) {
      myUserName = userName;
    }

    public static Context Create(@Nullable String userName) {
      Context newContext = new Context(userName);
      assert ourCurrentContext == null;
      ourCurrentContext = newContext;
      return newContext;
    }

    public void Close() {
      if (ourCurrentContext == this) {
        ourCurrentContext = null;
      }
    }
  }
}
