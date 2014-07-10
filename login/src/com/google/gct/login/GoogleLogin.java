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
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpRequestFactory;
import com.google.common.base.Strings;
import com.google.gct.login.ui.GoogleLoginActionButton;
import com.google.gct.login.ui.GoogleLoginCopyAndPasteDialog;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import com.google.gdt.eclipse.login.common.LoggerFacade;
import com.google.gdt.eclipse.login.common.OAuthData;
import com.google.gdt.eclipse.login.common.OAuthDataStore;
import com.google.gdt.eclipse.login.common.UiFacade;

import com.google.gdt.eclipse.login.common.VerificationCodeHolder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;

import net.jcip.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;


/**
 * Class that handles logging in to Google services.
 */
// TODO: explore changing class to an application service
public class GoogleLogin {

  private ClientInfo clientInfo;
  private AndroidUiFacade uiFacade;
  private AndroidPreferencesOAuthDataStore dataStore;
  private CredentialedUserRoster users;
  private static GoogleLogin instance;

  public static final Logger GOOGLE_LOGIN_LOG =  Logger.getInstance(GoogleLogin.class);

  /**
   * Constructor
   */
  private GoogleLogin() {
    this.clientInfo = getClientInfo();
    this.uiFacade = new AndroidUiFacade();
    this.users = new CredentialedUserRoster();
    this.dataStore =  new AndroidPreferencesOAuthDataStore();
    addLoginListenersFromExtensionPoints();
  }

  /**
   * Gets the {@link GoogleLogin} object.
   * @return the {@link GoogleLogin} object.
   */
  public static GoogleLogin getInstance() {
    if(instance == null) {
      instance = new GoogleLogin();
      instance.dataStore.initializeUsers();
    }
    return instance;
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
  public static void promptToLogIn(final String message, @Nullable final IGoogleLoginCompletedCallback callback)
    throws InvalidThreadTypeException {
    if (!instance.isLoggedIn()) {
      if(ApplicationManager.getApplication().isDispatchThread()) {
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
  public HttpRequestFactory createRequestFactory(String message) {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
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
  public String fetchAccessToken() throws IOException {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchAccessToken();
  }

  /**
   * Returns the OAuth2 Client ID for the active user.
   * @return the OAuth2 Client ID for the active user.
   */
  public String fetchOAuth2ClientId() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2ClientId();
  }

  /**
   * Returns the OAuth2 Client Secret for the active user.
   * @return the OAuth2 Client Secret for the active user.
   */
  public String fetchOAuth2ClientSecret() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
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
  public String fetchOAuth2RefreshToken() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
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
  public String fetchOAuth2Token() throws IOException {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().fetchOAuth2Token();
  }

  /**
   * Returns the credentials of the active user. If there is no active user,
   * returns credentials with the access token and refresh token set to null.
   * @return the OAuth credentials.
   */
  public Credential getCredential() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return null;
    }
    return activeUser.getGoogleLoginState().getCredential();
  }

  /**
   * Returns the active user's email address, or null if there is no active user,
   * @return the active user's email address, or null if there is no active user,
   */
  public String getEmail() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
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
   * See {@link #logIn(String)}.
   */
  public void logIn() {
    users.removeActiveUser();
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
  public void logIn(final String message, @Nullable final IGoogleLoginCompletedCallback callback) {
    final GoogleLoginState state = createGoogleLoginState();

    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        boolean loggedIn = state.logInWithLocalServer(message);

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
      }
    });
  }

  /**
   * Logs the user out. Pops up a question dialog asking if the user really
   * wants to quit.
   *
   * @return true if the user logged out, false otherwise
   */
  public boolean logOut() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return false;
    }

    boolean loggedOut =  activeUser.getGoogleLoginState().logOut();
    if(loggedOut) {
      users.removeUser(activeUser.getEmail());
    }

    return loggedOut;
  }

  /**
   * Logs the user out.
   *
   * @param showPrompt if true, opens a prompt asking if the user really wants
   *          to log out. If false, the user is logged out
   * @return true if the user was logged out or is already logged out, and false
   *         if the user chose not to log out
   */
  public boolean logOut(boolean showPrompt) {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return false;
    }
    return activeUser.getGoogleLoginState().logOut(showPrompt);
  }

  /**
   * Creates a new {@link Credential}. If there is an active user, populates
   * the newly created {@link Credential} with the active user's access and refresh tokens, else
   * the access and refresh token will be set to null.
   * @return a new {@link Credential}.
   */
  public Credential makeCredential() {
    CredentialedUser activeUser = users.getActiveUser();
    if(activeUser == null) {
      return null;
    } else {
      return activeUser.getGoogleLoginState().makeCredential();
    }
  }

  /**
   * Logs out the active user.
   */
  // TODO: Hook this into the shutdown process of IntelliJ
  public void stop() {
    if (GoogleLoginPrefs.getLogoutOnExitPref()) {
      logOut(false);
    }
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
  public Map<String, CredentialedUser> getAllUsers() {
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
   * Gets all the implementations of  {@link GoogleLoginListener} and registers them to
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
  private GoogleLoginState createGoogleLoginState() {
    GoogleLoginState state =
      new GoogleLoginState(
        clientInfo.getId(),
        clientInfo.getInfo(),
        OAuthScopeRegistry.getScopes(),
        new AndroidPreferencesOAuthDataStore(),
        uiFacade,
        new AndroidLoggerFacade());
    return state;
  }

  /**
   * Returns the Client Info for Android Studio in a {@link com.google.gct.login.GoogleLogin.ClientInfo}.
   * @return the Client Info for Android Studio in a {@link com.google.gct.login.GoogleLogin.ClientInfo}.
   */
  private ClientInfo getClientInfo() {
      String id = LoginContext.getId();
      String info = LoginContext.getInfo();
      if (id != null && id.trim().length() > 0
          && info != null && info.trim().length() > 0) {
        return new ClientInfo(id, info);
    }

    throw new IllegalStateException("The client information for Android Studio was not found");
  }

  // TODO: update code to specify parent
  private void logErrorAndDisplayDialog(@NotNull final String title, @NotNull final Exception exception) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      Messages.showErrorDialog(exception.getMessage(), title);
    } else {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(exception.getMessage(), title);
        }
      }, ModalityState.defaultModalityState());
    }

    GOOGLE_LOGIN_LOG.error(exception.getMessage(), exception);
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
    private GoogleLoginActionButton myButton;

    @Override
    public String obtainVerificationCodeFromUserInteraction(String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
      GoogleLoginCopyAndPasteDialog dialog = new GoogleLoginCopyAndPasteDialog(myButton, authCodeRequestUrl, "Google Login");
      dialog.show();
      if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
        return null;
      }

      return Strings.emptyToNull(dialog.getVerificationCode());
    }

    @Override
    public VerificationCodeHolder obtainVerificationCodeFromExternalUserInteraction(String title) {
      VerificationCodeReceiver receiver = new LocalServerReceiver();
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
      catch (IOException e) {
        logErrorAndDisplayDialog(title == null? "Google Login" : title, e);
        return null;
      }

      return new VerificationCodeHolder(verificationCode, redirectUrl);
    }

    @Override
    public void showErrorDialog(String title, String message) {
      Messages.showErrorDialog(message, title);
    }

    @Override
    public boolean askYesOrNo(String title, String message) {
      return (Messages.showYesNoDialog(message, title, Messages.getQuestionIcon()) == Messages.YES);
    }

    @Override
    public void notifyStatusIndicator() {
      if (myButton != null) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            myButton.updateUi();
          }
        });
      }
    }

    /**
     * Sets the login menu item.
     *
     * @param button The login menu item.
     */
    public void setLoginMenuItemContribution(GoogleLoginActionButton trim) {
      this.myButton = trim;
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
      SortedSet<String> allUsers = GoogleLoginPrefs.getStoredUsers();
      for(String aUser : allUsers) {
        // Add a new user, so that loadOAuth called from the GoogleLoginState constructor
        // will be able to create a customized key to get that user's OAuth data
        // This will be overwritten with new GoogleLoginState object
        users.addUser(new CredentialedUser(aUser));

        // CredentialedUser's credentials will be updated from the persistent storage in GoogleLoginState constructor
        GoogleLoginState delegate = createGoogleLoginState();
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
        users.setActiveUser(activeUserString);
      }
    }
  }

  private static class AndroidLoggerFacade implements LoggerFacade {
    @Override
    public void logError(String msg, Throwable t) {
      GOOGLE_LOGIN_LOG.error(msg, t);
    }

    @Override
    public void logWarning(String msg) {
      GOOGLE_LOGIN_LOG.warn(msg);
    }
  }
}
