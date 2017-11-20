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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.gdt.eclipse.login.common.GoogleLoginState;
import java.awt.Image;
import org.jetbrains.annotations.Nullable;

/**
 * Class that represents a single logged in user.
 */
public class CredentialedUser {
  private String name;
  private Image image;
  private final String email;
  private final Credential credential;
  private boolean isActive = false;
  private final GoogleLoginState googleLoginState;

  /**
   * Constructor. Should only be used as temporary place holder for user.
   * @param email Email address of user
   */
  protected CredentialedUser(String email) {
    this.email = email;
    name = "";
    credential = null;
    image = null;
    googleLoginState = null;
  }

  /**
   * Creates a credentialed user.
   */
  public CredentialedUser(GoogleLoginState state,
      @Nullable final IGoogleLoginCompletedCallback loginCompletedCallback) {
    this.email = state.getEmail();
    googleLoginState = state;
    credential = googleLoginState.makeCredential();
    GoogleLoginUtils
        .getUserInfo(credential,
            userInfoPlus -> initializeUserInfo(userInfoPlus, loginCompletedCallback));
  }

  /**
   * Returns the email address of this user.
   * @return Email address of user.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns the credential of this user.
   *
   * @return Credential of user.
   */
  public Credential getCredential() {
    return credential;
  }

  /**
   * Returns true if this user is the active user and false otherwise.
   *
   * @return True if this user is active and false otherwise.
   */
  public boolean isActive() {
    return isActive;
  }

  public String getName() {
    return name;
  }

  public Image getPicture() {
    return image;
  }

  public GoogleLoginState getGoogleLoginState() {
    return googleLoginState;
  }

  /**
   * Sets this user to active if <code>isActive</code> is true and false otherwise.
   * @param isActive True if this user should be set to active and false otherwise.
   */
  protected void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  private void initializeUserInfo(Userinfoplus userInfo,
      @Nullable final IGoogleLoginCompletedCallback loginCompletedCallback) {
    if (userInfo == null) {
      name = null;
      image = null;
    } else {
      name = userInfo.getName();
      GoogleLoginUtils.provideUserPicture(userInfo, newImage -> {
        image = newImage;
        if (loginCompletedCallback != null) {
          loginCompletedCallback.onLoginCompleted();
        }
      });
    }
  }
}
