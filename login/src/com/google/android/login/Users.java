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

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the list of logged in users.
 */
public class Users {
  private final Map<String, User> allUsers = new HashMap<String, User>();
  private User activeUser;

  /**
   * Returns a copy of the map of the current logged in users.
   * @return Copy of current logged in users.
   */
  public Map<String, User> getAllUsers() {
    synchronized (this) {
      Map<String, User> clone = new HashMap<String, User>();
      clone.putAll(allUsers);
      return clone;
    }
  }

  /**
   * Completely overrides current map of logged in users with <code>users</code>.
   * @param users Map of users to set.
   */
  public void setAllUsers(Map<String, User> users) {
    synchronized (this) {
      allUsers.clear();
      allUsers.putAll(users);
    }
  }

  /**
   * Returns the active user.
   * @return the active user.
   */
  public User getActiveUser() {
    synchronized (this) {
      return activeUser;
    }
  }

  /**
   * Sets the active user to <code>userEmail</code> if <code>userEmail</code> is a logged
   * in user.
   * @param userEmail The user to be set as active.
   * @return Returns true if <code>userEmail</code> was successfully set as the active user.
   * Returns false, if <code>userEmail</code> is not a logged in user.
   */
  public boolean setActiveUser(String userEmail) {
    synchronized (this) {
      if(!allUsers.containsKey(userEmail)) {
        return false;
      }

      if(activeUser != null) {
        activeUser.setActive(false);
      }

      activeUser = allUsers.get(userEmail);
      activeUser.setActive(true);
      // TODO: Send message out of changed user
      return true;
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
    synchronized (this) {
      return activeUser != null;
    }
  }

  /**
   * Adds a user to the list of current users.
   * The <code>user</code> becomes the active user.
   * @param user
   * @return
   */
  public boolean addUser(User user) {
    synchronized (this) {
      if(allUsers.containsKey(user.getEmail())) {
        return false;
      }

      allUsers.put(user.getEmail(), user);
      setActiveUser(user.getEmail());

      return true;
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
        // TODO: Send message out of changed user
      }

      allUsers.remove(userEmail);
      return true;
    }
  }

  /**
   * Clears the map of logged in users.
   */
  public void removeAllUsers(){
    synchronized (this) {
      allUsers.clear();
      activeUser = null;
      // TODO: Send message out of changed user
    }
  }
}
