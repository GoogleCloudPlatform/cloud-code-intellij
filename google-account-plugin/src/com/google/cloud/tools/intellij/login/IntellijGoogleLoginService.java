/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.login;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpRequestFactory;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginCopyAndPasteDialog;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginIcons;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.google.cloud.tools.intellij.stats.LoginTracking;
import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.common.base.Strings;
import com.google.gct.login.LoginContext;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gdt.eclipse.login.common.LoggerFacade;
import com.google.gdt.eclipse.login.common.OAuthData;
import com.google.gdt.eclipse.login.common.OAuthDataStore;
import com.google.gdt.eclipse.login.common.UiFacade;
import com.google.gdt.eclipse.login.common.VerificationCodeHolder;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import java.awt.Window;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.jcip.annotations.Immutable;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Class that handles logging in to Google services.
 */
public class IntellijGoogleLoginService implements GoogleLoginService {

  private static final Logger LOG = Logger.getInstance(IntellijGoogleLoginService.class);
  private ClientInfo clientInfo;
  private AndroidUiFacade uiFacade;
  private AndroidPreferencesOAuthDataStore dataStore;
  private CredentialedUserRoster users;

  private IntellijGoogleLoginService() {
    this.clientInfo = getClientInfo();
    this.uiFacade = new AndroidUiFacade();
    this.users = new CredentialedUserRoster();
    this.dataStore =  new AndroidPreferencesOAuthDataStore();
  }

  @Nullable
  private static Project getCurrentProject() {
    Window activeWindow = WindowManagerEx.getInstanceEx().getMostRecentFocusedWindow();
    if (activeWindow == null) {
      return null;
    }
    return CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(activeWindow));
  }

  /**
   * Returns the Client Info for Android Studio in a {@link ClientInfo}.
   */
  private static ClientInfo getClientInfo() {
    String id = LoginContext.getId();
    String info = LoginContext.getInfo();
    if (id != null && id.trim().length() > 0
        && info != null && info.trim().length() > 0) {
      return new ClientInfo(id, info);
    }

    throw new IllegalStateException("The client information for Android Studio was not found");
  }

  // TODO: update code to specify parent
  private static void logErrorAndDisplayDialog(
      @NotNull final String title,
      @NotNull final Exception exception) {
    LOG.error(exception.getMessage(), exception);
    GoogleLoginUtils.showErrorDialog(exception.getMessage(), title);
  }

  /**
   * Returns an HttpRequestFactory object that has been signed with the active user's authentication
   * headers to use to make http requests. If the user has not signed in, this method will block and
   * pop up the login dialog to the user. If the user cancels signing in, this method will return
   * null.
   * <p/>
   * If the access token that was used to sign this transport was revoked or has expired,
   * then execute() invoked on Request objects constructed from this transport will throw an
   * exception, for example, "com.google.api.client.http.HttpResponseException: 401 Unauthorized"
   *
   * @return An HttpRequestFactory object that has been signed with the active user's authentication
   *     headers or null if there is no active user.
   */
  @Nullable
  public HttpRequestFactory createRequestFactory() {
    return createRequestFactory(null);
  }

  @Override
  @Nullable
  public HttpRequestFactory createRequestFactory(@Nullable String message) {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      // TODO: prompt user to select an existing user or sign in
      return null;
    }
    return activeUser.getGoogleLoginState().createRequestFactory(message);
  }

  @Override
  @Nullable
  public Credential getCredential() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().getCredential();
  }

  @Override
  @Nullable
  public String getEmail() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().getEmail();
  }

  @Override
  public boolean isLoggedIn() {
    return users.isActiveUserAvailable();
  }

  @Override
  public void logInIfNot() {
    if (!isLoggedIn()) {
      logIn();
    }
  }

  @Override
  public void logIn() {
    logIn(null, null);
  }

  /**
   * Opens an external browser to allow the user to sign in. If the user is already signed in, this
   * updates the user's credentials. If the logging process fails, a message dialog will pop up to
   * notify the user. If the logging process succeeds, a logging event will be fired.
   *
   * @param message if not null, then this message is displayed above the login dialog. This is for
   *     when the user is presented the login dialog from doing something other than logging in,
   *     such as accessing Google API services. It should say something like "Importing a project
   *     from Google Project Hosting requires signing in."
   * @param loginCompletedCallback if not null, then this callback is called when the login either
   *     succeeds or fails
   *
   */
  @Override
  public void logIn(@Nullable final String message,
      @Nullable final IGoogleLoginCompletedCallback loginCompletedCallback) {
    UsageTrackerProvider.getInstance().trackEvent(LoginTracking.LOGIN_START).ping();

    final CredentialedUser lastActiveUser = users.getActiveUser();
    users.removeActiveUser();

    final GoogleLoginState state = createGoogleLoginState(false);

    // We pass in the current project, which causes intelliJ to properly figure out the
    // parent window. This keeps the cancel dialog on top and visible.
    new Task.Modal(
        getCurrentProject(),
        AccountMessageBundle.message("login.service.sign.in.via.browser.modal.text"), true) {
      private boolean loggedIn = false;

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        if (!(indicator instanceof ProgressIndicatorEx)) {
          return;
        }

        ((ProgressIndicatorEx)indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
          @Override
          public void cancel() {
            assert uiFacade != null;
            uiFacade.stop();
            super.cancel();
          }
        });

        loggedIn = state != null && state.logInWithLocalServer(message);
      }

      @Override
      public void onCancel() {
        notifyOnComplete();
      }

      @Override
      public void onSuccess() {
        notifyOnComplete();
      }

      private void notifyOnComplete() {
        // TODO: add user preference to chose to use pop-up copy and paste dialog
        if (loggedIn) {
          users.addUser(new CredentialedUser(state, () -> {
            if (loginCompletedCallback != null) {
              loginCompletedCallback.onLoginCompleted();
            }
          }));
        } else {
          // Login failed (or aborted), so restore the last active user, if any
          restoreLastActiveUser();

          if (loginCompletedCallback != null) {
            loginCompletedCallback.onLoginCompleted();
          }
        }
      }

      private void restoreLastActiveUser() {
        if (lastActiveUser != null) {
          setActiveUser(lastActiveUser.getEmail());
        }
      }
    }.queue();
  }

  @Override
  public boolean logOut(boolean showPrompt) {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return false;
    }

    boolean loggedOut = activeUser.getGoogleLoginState().logOut(showPrompt);
    if (loggedOut) {
      logOutAllUsers();
      UsageTrackerProvider.getInstance().trackEvent(LoginTracking.LOGOUT_COMPLETE).ping();
    }

    return loggedOut;
  }

  /**
   * Creates a new {@link Credential}. If there is an active user, populates
   * the newly created {@link Credential} with the active user's access and refresh tokens, else
   * the access and refresh token will be set to null.
   * @return a new {@link Credential}.
   */
  @Nullable
  public Credential makeCredential() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().makeCredential();
  }

  @Override
  public Map<String, CredentialedUser> getAllUsers() {
    return users.getAllUsers();
  }

  @NotNull
  @Override
  public Optional<CredentialedUser> getLoggedInUser(String username) {
    return Optional.ofNullable(getAllUsers().get(username));
  }

  @Override
  @Nullable
  public CredentialedUser getActiveUser() {
    return users.getActiveUser();
  }

  @Override
  public void setActiveUser(String userEmail) throws IllegalArgumentException {
    users.setActiveUser(userEmail);
  }

  @Override
  public void loadPersistedCredentials() {
    dataStore.initializeUsers();
  }

  @Override
  public boolean ensureLoggedIn(String username) {
    Optional<CredentialedUser> projectUser = getLoggedInUser(username);
    while (!projectUser.isPresent()) {
      int addUserResult = Messages.showOkCancelDialog(
          AccountMessageBundle.message("login.credentials.missing.message", username),
          AccountMessageBundle.message("login.credentials.missing.dialog.title"),
          AccountMessageBundle.message("login.credentials.missing.dialog.addaccount.button"),
          AccountMessageBundle.message("login.credentials.missing.dialog.cancel.button"),
          Messages.getWarningIcon());
      if (addUserResult == Messages.OK) {
        logIn();
        projectUser = getLoggedInUser(username);
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * Logs out all signed in users without popping up logout confirmation message.
   */
  private void logOutAllUsers() {
    for (CredentialedUser user : users.getAllUsers().values()) {
      user.getGoogleLoginState().logOut(false /* showPrompt */);
    }
    users.removeAllUsers();
  }

  /**
   * Creates a new instance of {@link GoogleLoginState}.
   * @return a new instance of {@link GoogleLoginState}.
   */
  @Nullable
  private GoogleLoginState createGoogleLoginState(boolean initializingUsers) {
    GoogleLoginState state = new GoogleLoginState(
        clientInfo.getId(),
        clientInfo.getInfo(),
        OAuthScopeRegistry.getScopes(),
        new AndroidPreferencesOAuthDataStore(),
        uiFacade,
        new AndroidLoggerFacade());

    if (initializingUsers && !state.isLoggedIn()) {
      // Logs user out if oauth scope for active user's credentials
      // does not match the current scope
      return null;
    }

    return state;
  }

  /**
   * The client information for an application.
   */
  @Immutable
  private static class ClientInfo {
    private final String id;
    private final String info;

    public ClientInfo(String id, String info) {
      this.id = id;
      this.info = info;
    }

    public String getId() {
      return id;
    }

    public String getInfo() {
      return info;
    }
  }

  private static class AndroidLoggerFacade implements LoggerFacade {
    @Override
    public void logError(String msg, Throwable throwable) {
      LOG.error(msg, throwable);
    }

    @Override
    public void logWarning(String msg) {
      LOG.warn(msg);
    }
  }

  /**
   * An implementation of {@link UiFacade} using Swing dialogs and external browsers.
   */
  private class AndroidUiFacade implements UiFacade {

    /**
     * number of characters to wrap lines after in the logout message.
     */
    public static final int WRAP_LENGTH = 60;
    private volatile CancellableServerReceiver receiver = null;
    private GoogleLoginMessageExtender[] messageExtenders;

    public AndroidUiFacade() {
      this.messageExtenders = Extensions.getExtensions(GoogleLoginMessageExtender.EP_NAME);
    }

    @Override
    public String obtainVerificationCodeFromUserInteraction(
        String title,
        GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
      GoogleLoginCopyAndPasteDialog dialog =
          new GoogleLoginCopyAndPasteDialog(
              authCodeRequestUrl,
              AccountMessageBundle.message("login.service.copyandpaste.title.text"));
      dialog.show();
      if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
        return null;
      }

      return Strings.emptyToNull(dialog.getVerificationCode());
    }

    public void stop() {
      CancellableServerReceiver localreceiver = receiver;
      if (localreceiver != null) {
        try {
          localreceiver.stop();
        } catch (IOException ioe) {
          logErrorAndDisplayDialog(
              AccountMessageBundle.message("login.service.default.error.dialog.title.text"), ioe);
        }
      }
    }

    @Override
    public VerificationCodeHolder obtainVerificationCodeFromExternalUserInteraction(String title) {
      receiver = new CancellableServerReceiver();
      String redirectUrl;
      try {
        redirectUrl = receiver.getRedirectUri();
      } catch (IOException ioe) {
        logErrorAndDisplayDialog(title == null
            ? AccountMessageBundle.message("login.service.default.error.dialog.title.text")
            : title, ioe);
        return null;
      }

      AuthorizationCodeRequestUrl authCodeRequestUrl =
          new AuthorizationCodeRequestUrl(
              GoogleOAuthConstants.AUTHORIZATION_SERVER_URL, clientInfo.getId())
              .setRedirectUri(redirectUrl)
              .setScopes(OAuthScopeRegistry.getScopes());

      BrowserUtil.browse(authCodeRequestUrl.build());

      String verificationCode;
      try {
        verificationCode = receiver.waitForCode();
      } catch (RequestCancelledException rce) {
        UsageTrackerProvider.getInstance().trackEvent(LoginTracking.LOGIN_CANCELLED).ping();
        return null;
      } catch (IOException ioe) {
        logErrorAndDisplayDialog(title == null
            ? AccountMessageBundle.message("login.service.default.error.dialog.title.text")
            : title, ioe);
        return null;
      } finally {
        receiver = null;
      }

      UsageTrackerProvider.getInstance().trackEvent(LoginTracking.LOGIN_COMPLETE).ping();
      return new VerificationCodeHolder(verificationCode, redirectUrl);
    }

    @Override
    public void showErrorDialog(final String title, final String message) {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(message, title);
        }
      });
    }

    @Override
    public boolean askYesOrNo(String title, String message) {
      StringBuilder updatedMessageBuilder = new StringBuilder(message);
      if (message.equals(AccountMessageBundle.message("login.service.are.you.sure.key.text"))) {
        updatedMessageBuilder.append(
            AccountMessageBundle.message("login.service.are.you.sure.append.text"));
        for (GoogleLoginMessageExtender messageExtender : messageExtenders) {
          String additionalLogoutMessage = messageExtender.additionalLogoutMessage();
          if (!Strings.isNullOrEmpty(additionalLogoutMessage)) {
            updatedMessageBuilder.append(" ").append(additionalLogoutMessage);
          }
        }
      }
      String updatedMessage = WordUtils.wrap(
          updatedMessageBuilder.toString(),
          WRAP_LENGTH,
          /* newLinestr */ null,
          /* wrapLongWords */ false);
      return (Messages.showYesNoDialog(
          updatedMessage, title, GoogleLoginIcons.GOOGLE_FAVICON) == Messages.YES);
    }

    @Override
    public void notifyStatusIndicator() {
    }
  }

  /**
   * An implementation of the {@link OAuthDataStore} interface using java preferences.
   */
  private class AndroidPreferencesOAuthDataStore implements OAuthDataStore {

    @Override
    public void saveOAuthData(OAuthData credentials) {
      GoogleLoginPrefs.saveOAuthData(credentials);
    }

    @Override
    public OAuthData loadOAuthData() {
      return GoogleLoginPrefs.loadOAuthData();
    }

    @Override
    public void clearStoredOAuthData() {
      GoogleLoginPrefs.clearStoredOAuthData();
    }

    public void initializeUsers() {
      String activeUserString = GoogleLoginPrefs.getActiveUser();
      List<String> allUsers = GoogleLoginPrefs.getStoredUsers();
      StringBuilder removedUsers = new StringBuilder();

      for (String user : allUsers) {
        // Add a new user, so that loadOAuth called from the GoogleLoginState constructor
        // will be able to create a customized key to get that user's OAuth data
        // This will be overwritten with new GoogleLoginState object
        users.addUser(new CredentialedUser(user));

        // CredentialedUser's credentials will be updated from the persistent storage
        // in GoogleLoginState constructor
        GoogleLoginState delegate = createGoogleLoginState(true);

        // delegate will be null if current scopes differ from scopes with users saved
        // auth credentials
        if (delegate == null) {
          removedUsers.append(user).append(", ");
          if (user.equals(activeUserString)) {
            activeUserString = null;
          }

          users.removeUser(user);
          continue;
        }

        users.addUser(new CredentialedUser(delegate, null /*loginCompletedCallback*/));
      }

      if (activeUserString == null) {
        users.removeActiveUser();
      } else {
        try {
          users.setActiveUser(activeUserString);
        } catch (IllegalArgumentException ex) {
          LOG.warn("Error while initiating users", ex);
          // Set no active user
          users.removeActiveUser();
        }
      }

      // Log removed users
      if (removedUsers.length() != 0) {
        LOG.info("The following user(s) had expired authentication scopes: "
            + removedUsers
            + "and have been logged out.");
      }
    }
  }
}
