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

package com.google.cloud.tools.intellij.login;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequestFactory;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginActionButton;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Provides Google user authentication services.
 */
public interface GoogleLoginService {

  /**
   * Returns an HttpRequestFactory object that has been signed with the active user's authentication
   * headers to use to make http requests. If the user has not signed in, this method will block and
   * pop up the login dialog to the user. If the user cancels signing in, this method will return
   * null.
   * <p/>
   * If the access token that was used to sign this transport was revoked or has expired, then
   * execute() invoked on Request objects constructed from this transport will throw an exception,
   * for example, "com.google.api.client.http.HttpResponseException: 401 Unauthorized"
   *
   * @param message The message to display in the login dialog if the user needs to log in to
   *     complete this action. If null, then no message area is created.
   * @return An HttpRequestFactory object that has been signed with the active user's authentication
   *     headers or null if there is no active user.
   */
  @Nullable
  HttpRequestFactory createRequestFactory(@Nullable String message);

  /**
   * Returns the credentials of the active user. If there is no active user,
   * returns credentials with the access token and refresh token set to {@code null}.
   */
  @Nullable
  Credential getCredential();

  /**
   * Returns the active user's email address, or {@code null} if there is no active user.
   */
  @Nullable
  String getEmail();

  /**
   * Verifies whether there is an active user of not.
   */
  boolean isLoggedIn();

  void logInIfNot();

  /**
   * See {@link #logIn(String, IGoogleLoginCompletedCallback)}.
   */
  void logIn();

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
   *     either succeeds or fails.
   */
  void logIn(@Nullable String message, @Nullable IGoogleLoginCompletedCallback callback);

  /**
   * Logs out the active user and all other signed in users.
   *
   * @param showPrompt if true, opens a prompt asking if the user really wants
   *          to log out. If false, the user is logged out
   * @return true if the user was logged out or is already logged out, and false
   *         if the user chose not to log out
   */
  boolean logOut(boolean showPrompt);

  /**
   * Sets the active user to <code>userEmail</code> if <code>userEmail</code> is a logged
   * in user.
   * @param userEmail The user to be set as active.
   * @throws IllegalArgumentException if the <code>userEmail</code> does not exist i.e. is
   *     not a logged in user.
   */
  void setActiveUser(String userEmail) throws IllegalArgumentException;

  /**
   * Returns a copy of the map of the current logged in users.
   */
  Map<String, CredentialedUser> getAllUsers();

  /**
   * Returns the active user.
   */
  @Nullable
  CredentialedUser getActiveUser();

  /**
   * When the login menu item is instantiated by the UI, it calls this method so that
   * when logIn() is called by something other than the login menu item itself, the
   * login menu item can be notified to update its UI.
   *
   * @param button The login menu item.
   */
  void setLoginMenuItemContribution(GoogleLoginActionButton button);
}
