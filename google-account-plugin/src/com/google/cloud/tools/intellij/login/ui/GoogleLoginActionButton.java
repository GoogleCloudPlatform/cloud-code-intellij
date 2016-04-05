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
package com.google.cloud.tools.intellij.login.ui;

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;

import org.jetbrains.annotations.NotNull;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;

/**
 * The Google Login button that appears on the main toolbar.
 */
public final class GoogleLoginActionButton extends ActionButton {
  private final static String SIGN_IN_MESSAGE = AccountMessageBundle.message(
      "login.toolbar.button.sign.in.text");

  public GoogleLoginActionButton(AnAction action, Presentation presentation, String place, @NotNull Dimension minimumSize) {
    super(action, presentation, place, minimumSize);
    Services.getLoginService().setLoginMenuItemContribution(this);
    updateUi();
  }

  /**
   * Updates the buttons tooltip and description text. The text is either going to be the
   * SIGN_IN_MESSAGE or the active user's email address.
   */
  public void updateUi() {
    CredentialedUser activeUser = Services.getLoginService().getActiveUser();
    if(activeUser == null) {
      myPresentation.setText(SIGN_IN_MESSAGE);
      myPresentation.setDescription(SIGN_IN_MESSAGE);
      myPresentation.setIcon(GoogleLoginIcons.DEFAULT_USER_AVATAR);
    } else {
      myPresentation.setText(activeUser.getEmail());
      myPresentation.setDescription(activeUser.getEmail());
      Image image = activeUser.getPicture();
      if(image == null) {
        myPresentation.setIcon(GoogleLoginIcons.DEFAULT_USER_AVATAR);
      } else {
        Image scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        myPresentation.setIcon(new ImageIcon(scaledImage));
      }
    }
  }
}
