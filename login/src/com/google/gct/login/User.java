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

/**
 * Class that represents a single logged in user.
 */
public class User {
  private String email = "";
  private boolean isActive = false;

  /**
   * Constructor
   * @param email Email address of user
   */
  public User(String email) {
    this.email = email;
  }

  /**
   * Returns the email address of this user.
   * @return Email address of user.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns true if this user is the active user and false otherwise.
   * @return True if this user is active and false otherwise.
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Sets this user to active if <code>isActive</code> is true and false otherwise.
   * @param isActive True if this user should be set to active and false otherwise.
   */
  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }
}
