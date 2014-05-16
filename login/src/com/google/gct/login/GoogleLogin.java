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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.gdt.eclipse.login.common.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.ui.Messages;
import net.jcip.annotations.Immutable;

import java.io.IOException;


/**
 * Class that handles logging in to Google services.
 */
public class GoogleLogin {

  private static final String CLIENT_ID = "ANDROID_CLIENT_ID";
  private static final String CLIENT_SECRET = "ANDROID_CLIENT_SECRET";
  private static AndroidUiFacade uiFacade;
  private static GoogleLogin instance;
  private final GoogleLoginState delegate;

  public static final Logger GOOGLE_LOGIN_LOG =  Logger.getInstance(GoogleLogin.class);

  static {
    ClientIdSecretPair clientInfo = getClientIdAndSecretFromExtensionPoints();
    uiFacade = new AndroidUiFacade();
    GoogleLoginState state =
      new GoogleLoginState(
        clientInfo.getClientId(),
        clientInfo.getClientSecret(),
        OAuthScopeRegistry.getScopes(),
        new AndroidPreferencesOAuthDataStore(),
        uiFacade,
        new AndroidLoggerFacade());
    addLoginListenersFromExtensionPoints(state);
    instance = new GoogleLogin(state);
  }


  /**
   * Constructor
   * @param delegate The {@link GoogleLoginState} for this application.
   */
  protected GoogleLogin(GoogleLoginState delegate) {
    this.delegate = delegate;
  }

  /**
   * Gets the {@link GoogleLogin} object.
   * @return the {@link GoogleLogin} object.
   */
  public static GoogleLogin getInstance() {
    return instance;
  }

  /**
   *  Displays a dialog to prompt the user to login into Google Services.
   */
  public static void promptToLogIn() {
    promptToLogIn(null);
  }

  /**
   * Displays a dialog to prompt the user to login into Google Services
   * @param message  If not null, this message would be the title of the dialog.
   */
  public static void promptToLogIn(final String message) {
    if (!instance.isLoggedIn()) {
      Application application = ApplicationManager.getApplication();
      Runnable promptToLoginTask = new Runnable() {
        @Override
        public void run() {
          GoogleLogin.getInstance().logIn(message);
        }
      };
      if (application.isDispatchThread()) {
        promptToLoginTask.run();
      }
      else {
        application.invokeAndWait(promptToLoginTask, ModalityState.defaultModalityState());
      }
    }
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
   * @return  An HttpRequestFactory object that has been signed with the active user's
   * authentication headers or null if there is no active user.
   */
  public HttpRequestFactory createRequestFactory() {
    return delegate.createRequestFactory(null);
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
    return delegate.createRequestFactory(message);
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh token
   * if it is expired.
   *
   * @return an OAuth2 token, or null if there was an error or if the user
   *         wasn't signed in or canceled signing in.
   * @throws IOException if something goes wrong while fetching the token.
   *
   */
  public String fetchAccessToken() throws IOException {
    return delegate.fetchAccessToken();
  }

  /**
   * Returns the OAuth2 Client ID for the active user.
   * @return the OAuth2 Client ID for the active user.
   */
  public String fetchOAuth2ClientId() {
    return delegate.fetchOAuth2ClientId();
  }

  /**
   * Returns the OAuth2 Client Secret for the active user.
   * @return the OAuth2 Client Secret for the active user.
   */
  public String fetchOAuth2ClientSecret() {
    return delegate.fetchOAuth2ClientSecret();
  }

  /**
   * Returns the OAuth2 refresh token for the active user, logging in to obtain it if necessary.
   * If there is no active user, this method blocks and prompts the user to log in or select
   * an already logged in user.
   *
   * @return the refresh token, or {@code null} if the user cancels out of a request to log in
   */
  public String fetchOAuth2RefreshToken() {
    return delegate.fetchOAuth2RefreshToken();
  }

  /**
   * Makes a request to get an OAuth2 access token from the OAuth2 refresh
   * token. This token is short lived.
   *
   * @return an OAuth2 token, or null if there was an error or if the user
   *         wasn't signed in and canceled signing in.
   * @throws IOException if something goes wrong while fetching the token.
   *
   */
  public String fetchOAuth2Token() throws IOException {
    return delegate.fetchOAuth2Token();
  }

  /**
   * Returns the credentials of the active user. If there is no active user,
   * returns credentials with the access token and refresh token set to null.
   * @return the OAuth credentials.
   */
  public Credential getCredential() {
    return delegate.getCredential();
  }

  /**
   * Returns the active user's email address, or the empty string if there is no active user,
   *         or null if the active user's email couldn't be retrieved
   * @return the active user's email address, or the empty string if there is no active user,
   *         or null if the active user's email couldn't be retrieved
   */
  public String getEmail() {
    return delegate.getEmail();
  }

  /**
   * Returns true if the plugin was able to connect to the internet to try to
   * verify the stored oauth credentials at start up.
   */
  public boolean isConnected() {
    return delegate.isConnected();
  }

  /**
   * Verifies whether there is an active user of not.
   * @return true if there is an active user, false otherwise.
   */
  public boolean isLoggedIn() {
    return delegate.isLoggedIn();
  }

  /**
   * See {@link #logIn(String)}.
   */
  public boolean logIn() {
    return delegate.logIn(null);
  }

  /**
   * Pops up the dialogs to allow the user to sign in. If the user is already
   * signed in, then this does nothing and returns true.
   *
   * @param message if not null, then this message is displayed above the
   *          login dialog. This is for when the user is presented
   *          the login dialog from doing something other than logging in, such
   *          as accessing Google API services. It should say something like
   *          "Importing a project from Google Project Hosting requires signing
   *          in."
   *
   * @return true if the user signed in or is already signed in, false otherwise
   */
  public boolean logIn(String message) {
    return delegate.logIn(message);
  }

  /**
   * Logs the user out. Pops up a question dialog asking if the user really
   * wants to quit.
   *
   * @return true if the user logged out, false otherwise
   */
  public boolean logOut() {
    return delegate.logOut();
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
    return delegate.logOut(showPrompt);
  }

  /**
   * Creates a new {@link Credential}. If there is an active user, populates
   * the newly created {@link Credential} with the active user's access and refresh tokens, else
   * the access and refresh token will be set to null.
   * @return a new {@link Credential}.
   */
  public Credential makeCredential() {
    return delegate.makeCredential();
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
   * Gets all the implementations of  {@link GoogleLoginListener} and registers them to
   * <code>state</code>.
   * @param state the {@link GoogleLoginState} for which we want to register listeners to.
   */
  private static void addLoginListenersFromExtensionPoints(GoogleLoginState state) {
    GoogleLoginListener[] loginListeners = Extensions.getExtensions(GoogleLoginListener.EP_NAME);
    for(GoogleLoginListener listener : loginListeners) {
      state.addLoginListener(listener);
    }
  }

  /**
   * Returns the OAuth 2.0 Client ID and Secret for Android Studio in a {@link ClientIdSecretPair}.
   * @return the OAuth 2.0 Client ID and Secret for Android Studio in a {@link ClientIdSecretPair}.
   */
  private static ClientIdSecretPair getClientIdAndSecretFromExtensionPoints() {
      String clientId = System.getenv().get(CLIENT_ID);
      String clientSecret = System.getenv().get(CLIENT_SECRET);
      if (clientId != null && clientId.trim().length() > 0
          && clientSecret != null && clientSecret.trim().length() > 0) {
        return new ClientIdSecretPair(clientId, clientSecret);
    }

    throw new IllegalStateException("The Google OAuth 2.0 Client id and/or secret for Android Studio was not found");
  }

  /**
   * A pair consisting of the OAuth client ID and OAuth client secret for a client application.
   */
  @Immutable
  private static class ClientIdSecretPair {
    private final String clientId;
    private final String clientSecret;

    public ClientIdSecretPair(String clientId, String clientSecret) {
      this.clientId = clientId;
      this.clientSecret = clientSecret;
    }

    public String getClientId() {
      return clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }
  }

  /**
   * An implementation of {@link UiFacade} using Swing dialogs and external browsers.
   */
  private static class AndroidUiFacade implements UiFacade {
    @Override
    public String obtainVerificationCodeFromUserInteraction(String title, GoogleAuthorizationCodeRequestUrl authCodeRequestUrl) {
      // TODO: implement this
      // TODO: add step to list all logged in users in the ui, so user can either log a new
      // user in or select an already signed in user.
      return null;
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
      // TODO: implement this
    }
  }

  /**
   * An implementation of the {@link OAuthDataStore} interface using java preferences.
   */
  private static class AndroidPreferencesOAuthDataStore implements OAuthDataStore {

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
