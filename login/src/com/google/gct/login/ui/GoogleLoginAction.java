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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.JComponent;

import java.awt.Point;

/**
 * Action to open the Google Login panel.
 */
public class GoogleLoginAction extends AnAction implements CustomComponentAction, RightAlignedToolbarAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    showPopup(e);
  }

  @Override
  public JComponent createCustomComponent(Presentation presentation) {
    return new GoogleLoginActionButton(this, presentation, presentation.getText(), ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
  }

  /**
   * Opens up the Google Login panel as a popup.
   */
  private void showPopup(AnActionEvent e) {
    GoogleLoginUsersPanel usersPanel = new GoogleLoginUsersPanel();
    ComponentPopupBuilder popup = JBPopupFactory.getInstance().createComponentPopupBuilder(usersPanel, usersPanel.getList());
    JComponent source = (JComponent)e.getInputEvent().getSource();
    popup.createPopup().show(new RelativePoint(source, new Point(0, source.getHeight() - 1)));
  }
}
