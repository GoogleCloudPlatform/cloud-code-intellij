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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.login.common.OAuthData;
import com.intellij.openapi.diagnostic.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class responsible for storing the active user's {@link OAuthData} object persistently,
 * retrieving it, and clearing it. Only the active user's data is managed at any given time.
 */
// TODO: see if PersistentStateComponent is a better way to store settings
public class GoogleLoginPrefs {

  // Delimiter for the list of scopes.
  private static final String DELIMITER = " ";
  private static String PREFERENCES_PATH = "/com/google/gct/login";
  private static final String OAUTH_DATA_EMAIL_KEY = "credentials_email";
  private static final String OAUTH_DATA_REFRESH_TOKEN_KEY = "credentials_refresh_token";
  private static final String ICON_ONLY_KEY = "icon_only";
  private static final String LOGOUT_ON_EXIT_KEY = "logout_on_exit";
  private static final String OAUTH_SCOPES_KEY = "oauth_scopes";
  private static final String USERS = "all_users";
  private static final String ACTIVE_USER = "active_user";

  public static final Logger LOG = Logger.getInstance(GoogleLoginPrefs.class);

  /**
   * Stores the specified {@link OAuthData} object for the active user persistently.
   *
   * @param credentials the specified {@code Credentials object}
   */
  public static void saveOAuthData(OAuthData credentials) {
    String userEmail = credentials.getStoredEmail();

    if (userEmail != null) {
      Preferences prefs = getPrefs();

      prefs.put(
          getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY, userEmail), credentials.getRefreshToken());

      // we save the scopes so that if the user updates the plugin and the
      // scopes change, we can force the plugin to log out.
      Joiner joiner = Joiner.on(DELIMITER);
      prefs.put(
          getCustomUserKey(OAUTH_SCOPES_KEY, userEmail),
          joiner.join(credentials.getStoredScopes()));

      prefs.put(getCustomUserKey(OAUTH_DATA_EMAIL_KEY, userEmail), userEmail);

      addUser(credentials.getStoredEmail());

      flushPrefs(prefs);
    }
  }

  /**
   * Retrieves the persistently stored {@link OAuthData} object for the active user, if any.
   *
   * @return the persistently stored {@code OAuthData} object for the active user if it exists or an
   *     {@code OAuthData} object all of whose getters return {@code null} .
   */
  public static OAuthData loadOAuthData() {
    String refreshToken = null;
    String storedEmail = null;
    SortedSet<String> storedScopes = new TreeSet<String>();

    if (Services.getLoginService().getActiveUser() != null) {
      Preferences prefs = getPrefs();

      refreshToken = prefs.get(getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY), null);
      storedEmail = prefs.get(getCustomUserKey(OAUTH_DATA_EMAIL_KEY), null);
      String storedScopesString = prefs.get(getCustomUserKey(OAUTH_SCOPES_KEY), "");

      // Use a set to ensure uniqueness.
      for (String scope : storedScopesString.split(DELIMITER)) {
        storedScopes.add(scope);
      }
    }

    return new OAuthData(null, refreshToken, storedEmail, storedScopes, 0);
  }

  /** Clears the persistently stored {@link OAuthData} object for the active user, if any. */
  public static void clearStoredOAuthData() {
    CredentialedUser activeUser = Services.getLoginService().getActiveUser();
    if (activeUser == null) {
      return;
    }

    Preferences prefs = getPrefs();
    prefs.remove(getCustomUserKey(OAUTH_DATA_REFRESH_TOKEN_KEY));
    prefs.remove(getCustomUserKey(OAUTH_DATA_EMAIL_KEY));
    prefs.remove(getCustomUserKey(OAUTH_SCOPES_KEY));
    removeUser(prefs, activeUser.getEmail());
    flushPrefs(prefs);
  }

  /**
   * Stores the specified preference of the active user to display only the icon in the login panel.
   *
   * @param logoutOnExit the preference of the active user to display only the icon in the login
   *     panel.
   */
  public static void saveIconOnlyPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(getCustomUserKey(ICON_ONLY_KEY), logoutOnExit);
    flushPrefs(prefs);
  }

  /**
   * Retrieves the persistently stored preference of the active user, if any, to logout on closing
   * Android Studio.
   *
   * @return the persistently stored preference of the active user, if any, to logout on closing
   *     Android Studio or false if preference does not exist.
   */
  public static boolean getLogoutOnExitPref() {
    return getPrefs().getBoolean(getCustomUserKey(LOGOUT_ON_EXIT_KEY), false);
  }

  /**
   * Stores the specified preference of the active user to logout on closing Android Studio.
   *
   * @param logoutOnExit the preference of the active user to logout on closing Android Studio.
   */
  public static void saveLogoutOnExitPref(boolean logoutOnExit) {
    Preferences prefs = getPrefs();
    prefs.putBoolean(getCustomUserKey(LOGOUT_ON_EXIT_KEY), logoutOnExit);
    flushPrefs(prefs);
  }

  /**
   * Retrieves the persistently stored list of users.
   *
   * @return the stored list of users.
   */
  @NotNull
  public static List<String> getStoredUsers() {
    Preferences prefs = getPrefs();
    String allUsersString = prefs.get(USERS, "");
    List<String> allUsers = new ArrayList<String>();
    if (allUsersString.isEmpty()) {
      return allUsers;
    }

    Splitter splitter = Splitter.on(DELIMITER).omitEmptyStrings();
    for (String user : splitter.split(allUsersString)) {
      allUsers.add(user);
    }
    return allUsers;
  }

  /**
   * Stores <code>user</code> as the active user.
   *
   * @param user The user to be stored as active.
   */
  public static void saveActiveUser(@NotNull String user) {
    Preferences prefs = getPrefs();
    prefs.put(ACTIVE_USER, user);
    flushPrefs(prefs);
  }

  /** Clears the persistently stored active user. */
  public static void removeActiveUser() {
    Preferences prefs = getPrefs();
    prefs.remove(ACTIVE_USER);
    flushPrefs(prefs);
  }

  /** Clears all persistently stored users. There is no active user after this. */
  public static void removeAllUsers() {
    Preferences prefs = getPrefs();
    prefs.remove(USERS);
    prefs.remove(ACTIVE_USER);
    flushPrefs(prefs);
  }

  /**
   * Retrieves the persistently stored active user.
   *
   * @return the stored active user.
   */
  @Nullable
  public static String getActiveUser() {
    Preferences prefs = getPrefs();
    String activeUser = prefs.get(ACTIVE_USER, null);
    if ((activeUser == null) || activeUser.isEmpty()) {
      return null;
    }
    return activeUser;
  }

  @VisibleForTesting
  public static String getPreferencesPath() {
    return PREFERENCES_PATH;
  }

  @VisibleForTesting
  public static void setPreferencesPath(String path) {
    PREFERENCES_PATH = path;
  }

  private static void flushPrefs(Preferences prefs) {
    try {
      prefs.flush();
    } catch (BackingStoreException ex) {
      LOG.error("Could not flush preferences while saving login credentials", ex);
    }
  }

  private static Preferences getPrefs() {
    return Preferences.userRoot().node(PREFERENCES_PATH);
  }

  private static String getCustomUserKey(String key) {
    CredentialedUser activeUser = Services.getLoginService().getActiveUser();
    if (activeUser == null) {
      return key;
    }

    return key + "_" + activeUser.getEmail();
  }

  private static String getCustomUserKey(String key, String userEmail) {
    if (userEmail == null) {
      return key;
    }

    return key + "_" + userEmail;
  }

  private static void addUser(String user) {
    Preferences prefs = getPrefs();
    String allUsersString = prefs.get(USERS, null);
    if (allUsersString == null) {
      prefs.put(USERS, user);
      return;
    }

    List<String> allUsers = new ArrayList<String>();
    Splitter splitter = Splitter.on(DELIMITER).omitEmptyStrings();
    for (String scope : splitter.split(allUsersString)) {
      allUsers.add(scope);
    }

    if (allUsers.contains(user)) {
      return;
    }

    Joiner joiner = Joiner.on(DELIMITER);
    prefs.put(USERS, joiner.join(allUsersString, user));
    flushPrefs(prefs);
  }

  private static void removeUser(Preferences prefs, String user) {
    String allUsersString = prefs.get(USERS, "");
    List<String> allUsers = Lists.newArrayList();
    for (String scope : allUsersString.split(DELIMITER)) {
      allUsers.add(scope);
    }

    allUsers.remove(user);
    Joiner joiner = Joiner.on(DELIMITER);
    prefs.put(USERS, joiner.join(allUsers));
  }
}
