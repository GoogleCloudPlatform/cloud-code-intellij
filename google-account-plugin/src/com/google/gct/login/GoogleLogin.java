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
package com.google.gct.login;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.gct.login.ui.GoogleLoginActionButton;
import com.google.gct.login.ui.GoogleLoginCopyAndPasteDialog;
import com.google.gct.login.ui.GoogleLoginIcons;
import com.google.gct.stats.LoginTracking;
import com.google.gct.stats.UsageTrackerProvider;
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

import net.jcip.annotations.Immutable;

import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Window;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * Class that handles logging in to Google services.
 */
// TODO: explore changing class to an application service
public class GoogleLogin {

  private ClientInfo clientInfo;
  private AndroidUiFacade uiFacade;
  private AndroidPreferencesOAuthDataStore dataStore;
  private CredentialedUserRoster users;

  private static volatile GoogleLogin instance;

  private static final Logger LOG = Logger.getInstance(GoogleLogin.class);

  private GoogleLogin() {
    this.clientInfo = getClientInfo();
    this.uiFacade = new AndroidUiFacade();
    this.users = new CredentialedUserRoster();
    this.dataStore =  new AndroidPreferencesOAuthDataStore();
    addLoginListenersFromExtensionPoints();
  }

  /**
   * Gets the {@link GoogleLogin} object.
   * 
   * @return the {@link GoogleLogin} object
   */
  @NotNull
  public static synchronized GoogleLogin getInstance() {
    if (instance == null) {
      instance = new GoogleLogin();
      instance.dataStore.initializeUsers();
    }
    return instance;
  }

  @VisibleForTesting
  public static void setInstance(GoogleLogin newInstance) {
    instance = newInstance;
  }

  /**
   *  Displays a dialog to prompt the user to login into Google Services.
   * @throws InvalidThreadTypeException
   */
  public static void promptToLogIn() throws InvalidThreadTypeException {
    promptToLogIn(null, null);
  }

  /**
   * Displays a dialog to prompt the user to login into Google Services
   * if there is current no active user. Does nothing if there is an active
   * user. This function must be called from the event dispatch thread (EDT).
   * @param message  If not null, this message would be the title of the dialog.
   * @param callback if not null, then this callback is called when the login
   * either succeeds or fails.
   * @throws InvalidThreadTypeException
   */
  public static void promptToLogIn(@Nullable final String message, @Nullable final IGoogleLoginCompletedCallback callback)
    throws InvalidThreadTypeException {
    if (!instance.isLoggedIn()) {
      if (ApplicationManager.getApplication().isDispatchThread()) {
        getInstance().logIn(message, callback);
      } else {
        throw new InvalidThreadTypeException("promptToLogin");
      }
    }
  }

  /**
   * Returns an HttpRequestFactory object that has been signed with the active user's
   * authentication headers to use to make http requests. If the user has not
   * signed in, this method will block and pop up the login dialog to the user.
   * If the user cancels signing in, this method will return null.
   *
   * If the access token that was used to sign this transport was revoked or
   * has expired, then execute() invoked on Request objects constructed from
   * this transport will throw an exception, for example,
   * "com.google.api.client.http.HttpResponseException: 401 Unauthorized"
   *
   * @return  An HttpRequestFactory object that has been signed with the active user's
   * authentication headers or null if there is no active user.
   */
  @Nullable
  public HttpRequestFactory createRequestFactory() {
    return createRequestFactory(null);
  }

  /**
   * Returns an HttpRequestFactory object that has been signed with the active user's
   * authentication headers to use to make http requests. If the user has not
   * signed in, this method will block and pop up the login dialog to the user.
   * If the user cancels signing in, this method will return null.
   *
   *  If the access token that was used to sign this transport was revoked or
   * has expired, then execute() invoked on Request objects constructed from
   * this transport will throw an exception, for example,
   * "com.google.api.client.http.HttpResponseException: 401 Unauthorized"
   *
   * @param message The message to display in the login dialog if the user needs
   *          to log in to complete this action. If null, then no message area
   *          is created. See {@link #logIn(String)}
   * @return  An HttpRequestFactory object that has been signed with the active user's
   * authentication headers or null if there is no active user.
   */
  @Nullable
  public HttpRequestFactory createRequestFactory(@Nullable String message) {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      // TODO: prompt user to select an existing user or sign in
      return null;
    }
    return activeUser.getGoogleLoginState().createRequestFactory(message);
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh token
   * if it is expired.
   *
   * @return an OAuth2 token, or null if there was an error or no active user
   * @throws IOException if something goes wrong while fetching the token.
   *
   */
  @Nullable
  public String fetchAccessToken() throws IOException {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchAccessToken();
  }

  /**
   * Returns the OAuth2 Client ID for the active user.
   * @return the OAuth2 Client ID for the active user.
   */
  @Nullable
  public String fetchOAuth2ClientId() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2ClientId();
  }

  /**
   * Returns the OAuth2 Client Secret for the active user.
   * @return the OAuth2 Client Secret for the active user.
   */
  @Nullable
  public String fetchOAuth2ClientSecret() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2ClientSecret();
  }

  /**
   * Returns the OAuth2 refresh token for the active user, logging in to obtain it if necessary.
   * If there is no active user, this method blocks and prompts the user to log in or select
   * an already logged in user.
   *
   * @return the refresh token, or {@code null} if the user cancels out of a request to log in
   */
  @Nullable
  public String fetchOAuth2RefreshToken() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2RefreshToken();
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh
   * token. This token is short lived.
   *
   * @return an OAuth2 token, or null if there was an error or if there is
   * active user.
   * @throws IOException if something goes wrong while fetching the token.
   *
   */
  @Nullable
  public String fetchOAuth2Token() throws IOException {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2Token();
  }

  /**
   * Returns the credentials of the active user. If there is no active user,
   * returns credentials with the access token and refresh token set to null.
   * @return the OAuth credentials.
   */
  @Nullable
  public Credential getCredential() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().getCredential();
  }

  /**
   * Returns the active user's email address, or null if there is no active user,
   * @return the active user's email address, or null if there is no active user,
   */
  @Nullable
  public String getEmail() {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().getEmail();
  }

  /**
   * Returns true if the plugin was able to connect to the internet to try to
   * verify the stored oauth credentials at start up or false otherwise.
   */
  public boolean isConnected() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return false;
    }
    return activeUser.getGoogleLoginState().isConnected();
  }

  /**
   * Verifies whether there is an active user of not.
   * @return true if there is an active user, false otherwise.
   */
  public boolean isLoggedIn() {
    return users.isActiveUserAvailable();
  }

  /**
   * See {@link #logIn(String, IGoogleLoginCompletedCallback)}.
   */
  public void logIn() {
    logIn(null, null);
  }

  /**
   * Opens an external browser to allow the user to sign in.
   * If the user is already signed in, this updates the user's credentials.
   * If the logging process fails, a message dialog will pop up to notify
   * the user. If the logging process succeeds, a logging event will be fired.
   *
   * @param message if not null, then this message is displayed above the
   *          login dialog. This is for when the user is presented
   *          the login dialog from doing something other than logging in, such
   *          as accessing Google API services. It should say something like
   *          "Importing a project from Google Project Hosting requires signing
   *          in."
   * @param callback if not null, then this callback is called when the login
   * either succeeds or fails.
   */
  public void logIn(@Nullable final String message, @Nullable final IGoogleLoginCompletedCallback callback) {
    UsageTrackerProvider.getInstance().trackEvent(LoginTracking.CATEGORY, LoginTracking.LOGIN, "login.start", null);
    users.removeActiveUser();
    uiFacade.notifyStatusIndicator();

    final GoogleLoginState state = createGoogleLoginState(false);

    // We pass in the current project, which causes intelliJ to properly figure out the parent window.
    // This keeps the cancel dialog on top and visible.
    new Task.Modal(getCurrentProject(), "Please sign in via the opened browser...", true) {
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
        if(loggedIn) {
          IGoogleLoginCompletedCallback localCallback = new IGoogleLoginCompletedCallback() {

            @Override
            public void onLoginCompleted() {
              uiFacade.notifyStatusIndicator();
              if(callback != null) {
                callback.onLoginCompleted();
              }
            }
          };
          users.addUser(new CredentialedUser(state, localCallback));
        }
        else if (callback != null) {
          callback.onLoginCompleted();
        }
      }
    }.queue();
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
   * Logs out the active user and all other signed in users.
   *
   * @param showPrompt if true, opens a prompt asking if the user really wants
   *          to log out. If false, the user is logged out
   * @return true if the user was logged out or is already logged out, and false
   *         if the user chose not to log out
   */
  public boolean logOut(boolean showPrompt) {
    CredentialedUser activeUser = users.getActiveUser();
    if (activeUser == null) {
      return false;
    }

    boolean loggedOut = activeUser.getGoogleLoginState().logOut(showPrompt);
    if(loggedOut) {
      logOutAllUsers();
      UsageTrackerProvider
          .getInstance().trackEvent(LoginTracking.CATEGORY, LoginTracking.LOGIN, "logout.complete", null);
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

  /**
   * Sets the active user to <code>userEmail</code> if <code>userEmail</code> is a logged
   * in user.
   * @param userEmail The user to be set as active.
   * @throws IllegalArgumentException if the <code>userEmail</code> does not exist i.e. is
   * not a logged in user.
   */
  public void setActiveUser(String userEmail) throws IllegalArgumentException {
    users.setActiveUser(userEmail);
    uiFacade.notifyStatusIndicator();
  }

  /**
   * Returns a copy of the map of the current logged in users.
   * @return Copy of current logged in users.
   */
  public LinkedHashMap<String, CredentialedUser> getAllUsers() {
    return users.getAllUsers();
  }

  /**
   * Returns the active user.
   * @return the active user.
   */
  @Nullable
  public CredentialedUser getActiveUser() {
    return users.getActiveUser();
  }

  /**
   * When the login menu item is instantiated by the UI, it calls this method so that
   * when logIn() is called by something other than the login menu item itself, the
   * login menu item can be notified to update its UI.
   *
   * @param button The login menu item.
   */
  public void setLoginMenuItemContribution(GoogleLoginActionButton button) {
    uiFacade.setLoginMenuItemContribution(button);
  }

  /**
   * Logs out all signed in users without popping up logout confirmation message.
   */
  private void logOutAllUsers() {
    for (CredentialedUser aUser : users.getAllUsers().values()) {
      aUser.getGoogleLoginState().logOut(false /* showPrompt */);
    }
    users.removeAllUsers();
  }

  /**
   * Gets all the implementations of {@link GoogleLoginListener} and registers them to
   * <code>state</code>.
   */
  private void addLoginListenersFromExtensionPoints() {
    GoogleLoginListener[] loginListeners = Extensions.getExtensions(GoogleLoginListener.EP_NAME);
    for(GoogleLoginListener listener : loginListeners) {
      users.addLoginListener(listener);
    }
  }

  /**
   * Creates a new instance of {@link GoogleLoginState}
   * @return a new instance of {@link GoogleLoginState}
   */
  @Nullable
  private GoogleLoginState createGoogleLoginState(boolean initializingUsers) {
    GoogleLoginState state =
      new GoogleLoginState(
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
   * Returns the Client Info for Android Studio in a {@link com.google.gct.login.GoogleLogin.ClientInfo}.
   * @return the Client Info for Android Studio in a {@link com.google.gct.login.GoogleLogin.ClientInfo}.
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
  private static void logErrorAndDisplayDialog(@NotNull final String title, @NotNull final Exception exception) {
    LOG.error(exception.getMessage(), exception);
    GoogleLoginUtils.showErrorDialog(exception.getMessage(), title);
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

  /**
   * An implementation of {@link UiFacade} using Swing dialogs and external browsers.
   */
  private class AndroidUiFacade implements UiFacade {

    /**
     * number of characters to wrap lines after in the logout message
     */
    public static final int WRAP_LENGTH = 60;
    private GoogleLoginActionButton button;
    private volatile CancellableServerReceiver receiver = null;
    private GoogleLoginMessageExtender[] messageExtenders;

    public AndroidUiFacade() {
      this.messageExtenders = Extensions.getExtensions(GoogleLoginMessageExtender.EP_NAME);
    }

    @Override
    public String obtainVerificationCodeFromUserInteraction(String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
      GoogleLoginCopyAndPasteDialog dialog = new GoogleLoginCopyAndPasteDialog(button, authCodeRequestUrl, "Google Login");
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
        }
        catch(IOException e) {
          logErrorAndDisplayDialog("Google Login", e);
        }
      }
    }

    @Override
    public VerificationCodeHolder obtainVerificationCodeFromExternalUserInteraction(String title) {
      receiver = new CancellableServerReceiver();
      String redirectUrl;
      try {
        redirectUrl = receiver.getRedirectUri();
      }
      catch (IOException e) {
        logErrorAndDisplayDialog(title == null? "Google Login" : title, e);
        return null;
      }

      AuthorizationCodeRequestUrl authCodeRequestUrl =
        new AuthorizationCodeRequestUrl(GoogleOAuthConstants.AUTHORIZATION_SERVER_URL, clientInfo.getId())
          .setRedirectUri(redirectUrl)
          .setScopes(OAuthScopeRegistry.getScopes());

      BrowserUtil.browse(authCodeRequestUrl.build());

      String verificationCode;
      try {
        verificationCode = receiver.waitForCode();
      }
      catch (RequestCancelledException e) {
        UsageTrackerProvider.getInstance().trackEvent(LoginTracking.CATEGORY, LoginTracking.LOGIN, "login.cancelled", null);
        return null;
      }
      catch (IOException e) {
        logErrorAndDisplayDialog(title == null ? "Google Login" : title, e);
        return null;
      }
      finally {
        receiver = null;
      }

      UsageTrackerProvider.getInstance().trackEvent(LoginTracking.CATEGORY, LoginTracking.LOGIN, "login.complete", null);
      return new VerificationCodeHolder(verificationCode, redirectUrl);
    }

    @Override
    public void showErrorDialog(String title, String message) {
      Messages.showErrorDialog(message, title);
    }

    @Override
    public boolean askYesOrNo(String title, String message) {
      String updatedMessage = message;
      if (message.equals("Are you sure you want to sign out?")) {
        updatedMessage += " This will sign out all logged in users.";
        for (GoogleLoginMessageExtender messageExtender : messageExtenders) {
          String additionalLogoutMessage = messageExtender.additionalLogoutMessage();
          if (!Strings.isNullOrEmpty(additionalLogoutMessage)) {
            updatedMessage += " " + additionalLogoutMessage;
          }
        }
      }
      updatedMessage = WordUtils.wrap(updatedMessage, WRAP_LENGTH, /* newLinestr */ null, /* wrapLongWords */ false);
      return (Messages.showYesNoDialog(updatedMessage, title, GoogleLoginIcons.GOOGLE_FAVICON) == Messages.YES);
    }

    @Override
    public void notifyStatusIndicator() {
      if (button != null) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            button.updateUi();
          }
        });
      }
    }

    /**
     * Sets the login menu item.
     *
     * @param trim the login menu item
     */
    public void setLoginMenuItemContribution(GoogleLoginActionButton trim) {
      this.button = trim;
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
      String removedUsers = "";

      for (String aUser : allUsers) {
        // Add a new user, so that loadOAuth called from the GoogleLoginState constructor
        // will be able to create a customized key to get that user's OAuth data
        // This will be overwritten with new GoogleLoginState object
        users.addUser(new CredentialedUser(aUser));

        // CredentialedUser's credentials will be updated from the persistent storage in GoogleLoginState constructor
        GoogleLoginState delegate = createGoogleLoginState(true);

        // delegate will be null if current scopes differ from scopes with users saved auth credentials
        if(delegate == null) {
          removedUsers += aUser + ", ";
          if(aUser.equals(activeUserString)) {
            activeUserString = null;
          }

          users.removeUser(aUser);
          continue;
        }

        IGoogleLoginCompletedCallback callback = new IGoogleLoginCompletedCallback() {
          @Override
          public void onLoginCompleted() {
            uiFacade.notifyStatusIndicator();
          }
        };

        users.addUser(new CredentialedUser(delegate, callback));
      }

      if(activeUserString == null) {
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
      if (!removedUsers.isEmpty()) {
        LOG.info("The following user(s) had expired authentication scopes: "
            + removedUsers
            + "and have been logged out.");
      }
    }
  }

  private static class AndroidLoggerFacade implements LoggerFacade {
    @Override
    public void logError(String msg, Throwable t) {
      LOG.error(msg, t);
    }

    @Override
    public void logWarning(String msg) {
      LOG.warn(msg);
    }
  }
}
