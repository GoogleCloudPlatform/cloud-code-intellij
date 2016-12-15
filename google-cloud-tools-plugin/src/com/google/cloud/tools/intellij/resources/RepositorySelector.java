/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.resources;

import com.google.cloud.tools.intellij.ui.CustomizableComboBox;
import com.google.cloud.tools.intellij.ui.CustomizableComboBoxPopup;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.JPanel;

/**
 * Created by eshaul on 12/14/16.
 */
public class RepositorySelector extends CustomizableComboBox implements CustomizableComboBoxPopup {

  private JBPopup popup;
  private RepositoryPanel panel;

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (popup == null || popup.isDisposed()) {
      panel = new RepositoryPanel();

      ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(panel, null); // todo change focus param
      popup = popupBuilder.createPopup();
    }
    if (!popup.isVisible()) {
      popup.show(showTarget);
    }
  }

  @Override
  public void hidePopup() {
    if (isPopupVisible()) {
      popup.closeOk(null);
    }
  }

  @Override
  public boolean isPopupVisible() {
    return false;
  }

  @Override
  protected CustomizableComboBoxPopup getPopup() {
    return this;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return 240;
  }

  private static class RepositoryPanel extends JPanel {

  }
}
