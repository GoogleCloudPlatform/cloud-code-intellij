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

package com.google.cloud.tools.intellij.login.ui;

import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.util.ui.JBUI;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

/** Action to open the Google Login panel. */
public class GoogleLoginAction extends AnAction implements DumbAware, RightAlignedToolbarAction {

  private static final String SIGN_IN_MESSAGE =
      AccountMessageBundle.message("login.toolbar.button.sign.in.text");
  private static final int ICON_SIZE = 16;

  /** Returns a new instance with the icon set to {@link GoogleLoginIcons#DEFAULT_USER_AVATAR}. */
  public GoogleLoginAction() {
    super(GoogleLoginIcons.DEFAULT_USER_AVATAR);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    showPopup(event);
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    CredentialedUser activeUser = Services.getLoginService().getActiveUser();
    if (activeUser == null) {
      presentation.setText(SIGN_IN_MESSAGE);
      presentation.setIcon(GoogleLoginIcons.DEFAULT_USER_AVATAR);
    } else {
      presentation.setText(activeUser.getEmail());
      Image image = activeUser.getPicture();
      if (image == null) {
        presentation.setIcon(GoogleLoginIcons.DEFAULT_USER_AVATAR);
      } else {
        int size = JBUI.scale(ICON_SIZE);
        Image scaledImage = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        presentation.setIcon(new ImageIcon(scaledImage));
      }
    }
  }

  /** Opens up the Google Login panel as a popup. */
  private static void showPopup(AnActionEvent event) {
    GoogleLoginUsersPanel usersPanel = new GoogleLoginUsersPanel();
    ComponentPopupBuilder popupBuilder =
        JBPopupFactory.getInstance().createComponentPopupBuilder(usersPanel, usersPanel.getList());
    JBPopup popup = popupBuilder.setCancelOnWindowDeactivation(true).createPopup();
    JComponent source = (JComponent) event.getInputEvent().getSource();
    popup.showUnderneathOf(source);
  }
}
