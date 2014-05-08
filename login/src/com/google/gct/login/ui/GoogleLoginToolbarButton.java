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
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The Google Login button on the Main toolbar that opens the Google Login panel.
 */
// TODO: replace with an icon with no text
public class GoogleLoginToolbarButton extends JButton {
  private Icon defaultIcon;
  private final static String SIGN_IN_MESSAGE = "Sign in to Google...";
  private final static String DEFAULT_AVATAR = "/icons/loginAvatar.png";

  public GoogleLoginToolbarButton() {
    GoogleLogin.getInstance().setLoginMenuItemContribution(this);

    defaultIcon = IconLoader.getIcon(DEFAULT_AVATAR);
    updateUi();

    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        showPopup();
      }
    });
  }

  /**
   * Opens up the Google Login panel as a popup.
   */
  private void showPopup() {
    GoogleLoginUsersPanel usersPanel = new GoogleLoginUsersPanel();
    ComponentPopupBuilder popup = JBPopupFactory.getInstance().createComponentPopupBuilder(usersPanel, usersPanel.getList());
    popup.createPopup().show(new RelativePoint(this, new Point(0, this.getHeight() - 1)));
  }

  /**
   * Updates the buttons text. The text is either going to be the SIGN_IN_MESSAGE or the active
   * user's email address.
   */
  public void updateUi() {
    CredentialedUser activeUser = GoogleLogin.getInstance().getActiveUser();
    if(activeUser == null) {
      setText(SIGN_IN_MESSAGE);
      setIcon(defaultIcon);
    } else {
      setText(activeUser.getEmail());
      Image image = activeUser.getPicture();
      if(image == null) {
        setIcon(defaultIcon);
      } else {
        Image scaledImage = image.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
        setIcon(new ImageIcon(scaledImage));
      }
    }
  }

}
