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
package com.google.android.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.gdt.eclipse.login.common.OAuthData;

import com.intellij.openapi.diagnostic.Logger;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The class responsible for storing the active user's {@link OAuthData}
 * object persistently, retrieving it, and clearing it. Only the active
 * user's data is managed at any given time.
 */
public class GoogleLoginPrefs {
  // Delimiter for the list of scopes.
  private static final String SCOPE_DELIMITER = " ";

  private static final String PREFERENCES_PATH = "/com/google/android/login";
  private static String preferencesPath = PREFERENCES_PATH;

  private static final String OAUTH_DATA_EMAIL_KEY = "credentials_email";
  private static final String OAUTH_DATA_ACCESS_TOKEN_KEY = "credentials_access_token";
  private static final String OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY =
    "credentials_access_token_expiry_time";
  private static final String OAUTH_DATA_REFRESH_TOKEN_KEY = "credentials_refresh_token";
  private static final String ICON_ONLY_KEY = "icon_only";
  private static final String LOGOUT_ON_EXIT_KEY = "logout_on_exit";
  private static final String OAUTH_SCOPES_KEY = "oauth_scopes";

  public static final Logger LOG =  Logger.getInstance(GoogleLoginPrefs.class);

  /**
   * Stores the specified {@link OAuthData} object for the active user persistently.
   * @param credentials the specified {@code Credentials object}
   */
  public static void saveOAuthData(OAuthData credentials) {
    Preferences prefs = getPrefs();
    prefs.put(getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_KEY), credentials.getAccessToken());
    prefs.put(getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY), credentials.getRefreshToken());
    prefs.put(
      getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY),
      Long.toString(credentials.getAccessTokenExpiryTime()));

    // we save the scopes so that if the user updates the plugin and the
    // scopes change, we can force the plugin to log out.
    Joiner joiner = Joiner.on(SCOPE_DELIMITER);
    prefs.put(getCustomUserKey(OAUTH_SCOPES_KEY), joiner.join(credentials.getStoredScopes()));

    String storedEmail = credentials.getStoredEmail();
    if (storedEmail != null) {
      prefs.put(getCustomUserKey(OAUTH_DATA_EMAIL_KEY), storedEmail);
    }
    flushPrefs(prefs);
  }

  /**
   * Retrieves the persistently stored {@link OAuthData} object for the active user, if any.
   * @return the persistently stored {@code OAuthData} object for the active user if it exists or
   * an {@code OAuthData} object all of whose getters return {@code null} .
   */
  public static OAuthData loadOAuthData() {
    Preferences prefs = getPrefs();

    String accessToken = prefs.get(getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_KEY), null);
    String refreshToken = prefs.get(getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY), null);
    String storedEmail = prefs.get(getCustomUserKey(OAUTH_DATA_EMAIL_KEY), null);
    String storedScopesString = prefs.get(getCustomUserKey(OAUTH_SCOPES_KEY), null);

    // Use a set to ensure uniqueness.
    SortedSet<String> storedScopes = new TreeSet<String>();
    if (storedScopesString != null) {
      for (String scope : storedScopesString.split(SCOPE_DELIMITER)) {
        storedScopes.add(scope);
      }
    }
    long accessTokenExpiryTime = 0;
    String accessTokenExpiryTimeString = prefs.get(getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY), null);
    if (accessTokenExpiryTimeString != null) {
      accessTokenExpiryTime = Long.parseLong(accessTokenExpiryTimeString);
    }
    return new OAuthData(
      accessToken, refreshToken, storedEmail, storedScopes, accessTokenExpiryTime);
  }

  /**
   * Clears the persistently stored {@link OAuthData} object for the active user, if any.
   */
  public static void clearStoredOAuthData() {
    Preferences prefs = getPrefs();
    prefs.remove(getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_KEY));
    prefs.remove(getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY));
    prefs.remove(getCustomUserKey(OAUTH_DATA_EMAIL_KEY));
    prefs.remove(getCustomUserKey(OAUTH_SCOPES_KEY));
    prefs.remove(getCustomUserKey(OAUTH_DATA_ACCESS_TOKEN_EXPIRY_TIME_KEY));
    flushPrefs(prefs);
  }

  /**
   * Stores the specified preference of the active user to display only the icon in the login panel.
   * @param logoutOnExit the preference of the active user to display only the icon in the
   *                     login panel.
   */
  public static void saveIconOnlyPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(getCustomUserKey(ICON_ONLY_KEY), logoutOnExit);
    flushPrefs(prefs);
  }

  /**
   * Retrieves the persistently stored preference of the active user, if any, to
   * logout on closing Android Studio.
   * @return  the persistently stored preference of the active user, if any, to
   * logout on closing Android Studio or false if preference does not exist.
   */
  public static boolean getLogoutOnExitPref() {
    return getPrefs().getBoolean(getCustomUserKey(LOGOUT_ON_EXIT_KEY), false);
  }

  /**
   * Stores the specified preference of the active user to logout on closing Android Studio.
   * @param logoutOnExit the preference of the active user to logout on closing Android Studio.
   */
  public static void saveLogoutOnExitPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(getCustomUserKey(LOGOUT_ON_EXIT_KEY), logoutOnExit);
    flushPrefs(prefs);
  }

  @VisibleForTesting
  public static void setTestPreferencesPath(String testPreferencesPath) {
    preferencesPath = testPreferencesPath;
  }

  private static void flushPrefs(Preferences prefs) {
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      LOG.error("Could not flush preferences while saving login credentials", e);
    }
  }

  private static Preferences getPrefs() {
    return Preferences.userRoot().node(preferencesPath);
  }

  private static String getCustomUserKey(String key) {

    // ToDo: uncomment when merged User object
    /**
     User activeUser = GoogleLogin.getInstance().getActiveUser();
     if(activeUser == null) {
     return key;
     }

     return key + "_" + activeUser.getEmail();
     **/
    return key;
  }
}
