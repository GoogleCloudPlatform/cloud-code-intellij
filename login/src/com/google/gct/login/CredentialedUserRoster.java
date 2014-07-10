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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which users are logged in, and which of these (if any) is currently designated as the
 * "active user". Maintains a map from email addresses of logged-in users to the corresponding
 * {@link CredentialedUser} objects.
 */
public class CredentialedUserRoster {
  private final Map<String, CredentialedUser> allUsers = new HashMap<String, CredentialedUser>();
  private CredentialedUser activeUser;
  private Collection<GoogleLoginListener> listeners;

  public CredentialedUserRoster() {
    listeners = Lists.newLinkedList();
  }

  /**
   * Returns a copy of the map of the current logged in users.
   * @return Copy of current logged in users.
   */
  public Map<String, CredentialedUser> getAllUsers() {
    synchronized (this) {
      Map<String, CredentialedUser> clone = new HashMap<String, CredentialedUser>();
      clone.putAll(allUsers);
      return clone;
    }
  }

  /**
   * Completely overrides current map of logged in users with <code>users</code>.
   * @param users Map of users to set.
   */
  public void setAllUsers(Map<String, CredentialedUser> users) {
    synchronized (this) {
      allUsers.clear();
      allUsers.putAll(users);
    }
  }

  /**
   * Returns the active user.
   * @return the active user.
   */
  @Nullable
  public CredentialedUser getActiveUser() {
    return activeUser;
  }

  /**
   * Sets the active user to <code>userEmail</code> if <code>userEmail</code> is a logged
   * in user.
   * @param userEmail The user to be set as active.
   * @throws IllegalArgumentException if the <code>userEmail</code> does not exist i.e. is
   * not a logged in user.
   */
  public void setActiveUser(@NotNull String userEmail) throws IllegalArgumentException {
    synchronized (this) {
      if(!allUsers.containsKey(userEmail)) {
        throw new IllegalArgumentException(userEmail + " is not a logged in user.");
      }

      if(activeUser != null) {
        activeUser.setActive(false);
      }

      activeUser = allUsers.get(userEmail);
      activeUser.setActive(true);
      GoogleLoginPrefs.saveActiveUser(userEmail);
      notifyLoginStatusChange();
    }
  }

  /**
   * If there is an active user, makes the active use no longer active.
   */
  public void removeActiveUser() {
    synchronized (this) {
      if(activeUser != null) {
        activeUser.setActive(false);
        activeUser = null;
        GoogleLoginPrefs.removeActiveUser();
        notifyLoginStatusChange();
      }
    }
  }

  /**
   * Returns the number of logged in users.
   * @return Number of logged in users.
   */
  public int numberOfUsers() {
    synchronized (this) {
      return allUsers.size();
    }
  }

  /**
   * Returns true if there is an active user and false otherwise.
   * @return True if there is an active user and false otherwise.
   */
  public boolean isActiveUserAvailable() {
    return activeUser != null;
  }

  /**
   * Adds a user to the list of current users. If the user already exists,
   * the user will be updated.
   * The <code>user</code> becomes the active user.
   * @param user
   * @return
   */
  public void addUser(CredentialedUser user) {
    synchronized (this) {
      allUsers.put(user.getEmail(), user);
      setActiveUser(user.getEmail());
    }
  }

  /**
   * Remove <code>userEmail</code> from the list of logged in users if <code>userEmail</code> is
   * a logged in user. If <code>userEmail</code> is the active user, there would no longer be
   * an active user once <code>userEmail</code> is removed. Another user will have to explicitly
   * selected for there to be an active user again.
   * @param userEmail The user to be removed.
   * @return  True if <code>userEmail</code> was successfully removed from the list of
   * logged in users and false if <code>userEmail</code> is not a logged in user.
   */
  public boolean removeUser(String userEmail) {
    synchronized (this) {
      if(!allUsers.containsKey(userEmail)) {
        return false;
      }

      if(activeUser.getEmail().equals(userEmail)) {
        activeUser = null;
        GoogleLoginPrefs.removeActiveUser();
      }

      allUsers.remove(userEmail);
      notifyLoginStatusChange();
      return true;
    }
  }

  /**
   * Register a specified {@link GoogleLoginListener} to be notified of changes to the
   * logged-in state.
   *
   * @param listener the specified {@code GoogleLoginListener}
   */
  void addLoginListener(GoogleLoginListener listener) {
    synchronized(listeners) {
      listeners.add(listener);
    }
  }

  private void notifyLoginStatusChange() {
    synchronized(listeners) {
      for (GoogleLoginListener listener : listeners) {
        listener.statusChanged();
      }
    }
  }

}
