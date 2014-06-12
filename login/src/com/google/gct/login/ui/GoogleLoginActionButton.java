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
package com.google.gct.login.ui;

import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Image;

/**
 * The Google Login button that appears on the main toolbar.
 */
public class GoogleLoginActionButton extends ActionButton {
  private Icon defaultIcon;
  private final static String SIGN_IN_MESSAGE = "Sign in to Google...";
  private final static String DEFAULT_AVATAR = "/icons/loginAvatar.png";

  public GoogleLoginActionButton(AnAction action, Presentation presentation, String place, @NotNull Dimension minimumSize) {
    super(action, presentation, place, minimumSize);

    GoogleLogin.getInstance().setLoginMenuItemContribution(this);
    defaultIcon = IconLoader.getIcon(DEFAULT_AVATAR);
    updateUi();
  }

  /**
   * Updates the buttons tooltip and description text. The text is either going to be the
   * SIGN_IN_MESSAGE or the active user's email address.
   */
  public void updateUi() {
    CredentialedUser activeUser = GoogleLogin.getInstance().getActiveUser();
    if(activeUser == null) {
      setToolTipText(SIGN_IN_MESSAGE);
      myPresentation.setDescription(SIGN_IN_MESSAGE);
      myPresentation.setIcon(defaultIcon);
    } else {
      setToolTipText(activeUser.getEmail());
      myPresentation.setDescription(activeUser.getEmail());
      Image image = activeUser.getPicture();
      if(image == null) {
        myPresentation.setIcon(defaultIcon);
      } else {
        Image scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        myPresentation.setIcon(new ImageIcon(scaledImage));
      }
    }
  }
}
