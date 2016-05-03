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

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import java.awt.Point;

import javax.swing.JComponent;

/**
 * Action to open the Google Login panel.
 */
public class GoogleLoginAction extends AnAction
    implements DumbAware, CustomComponentAction, RightAlignedToolbarAction {

  @Override
  public void actionPerformed(AnActionEvent event) {
    showPopup(event);
  }

  @Override
  public JComponent createCustomComponent(Presentation presentation) {
    return new GoogleLoginActionButton(
        this, presentation, presentation.getText(), ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
  }

  /**
   * Opens up the Google Login panel as a popup.
   */
  private static void showPopup(AnActionEvent event) {
    GoogleLoginUsersPanel usersPanel = new GoogleLoginUsersPanel();
    JComponent source = (JComponent)event.getInputEvent().getSource();
    ComponentPopupBuilder popupBuilder =
        JBPopupFactory.getInstance().createComponentPopupBuilder(usersPanel, usersPanel.getList());
    JBPopup popup = popupBuilder.createPopup();
    JComponent component = popup.getContent();
    int startingPoint = (int)(source.getWidth() - component.getPreferredSize().getWidth());
    popup.show(new RelativePoint(source, new Point(startingPoint, source.getHeight() - 1)));
  }
}
