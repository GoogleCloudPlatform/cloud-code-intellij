/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.cloudlogging;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

import java.awt.*;

import javax.swing.*;


/**
 * Renderer to set the text and Icon for each element in the Combo Box for Log levels
 * Created by amulyau on 7/1/15.
 */
public class LogLevelComboBoxRenderer extends JBLabel implements ListCellRenderer{

  private final Color backgroundNonSelectionColor = UIUtil.getListBackground(false);
  private final Color  backgroundSelectionColor = UIUtil.getListBackground(true);

  private final Color textNonSelectColor = UIUtil.getListForeground(false);
  private final Color textSelectColor = UIUtil.getListForeground(true);

  AppEngineLogToolWindowView view;

  public LogLevelComboBoxRenderer(AppEngineLogToolWindowView view) {

    this.view = view;
  }

  /**
   * Each element is a JLabel with text and an icon
   * @param list The Jlist
   * @param value Value of the object which is a JBLabel
   * @param index Index of the object in the list
   * @param isSelected Boolean value on whether the item is selected or not
   * @param cellHasFocus Boolean value on whether the item has focus or not.
   * @return JBLabel
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index,
                                                boolean isSelected, boolean cellHasFocus) {
    JBLabel label = ((JBLabel)value); //value passed in is jb label
    setOpaque(true);

    setText(label.getText());
    setIcon(label.getIcon());
    if (isSelected) {
      setBackground(backgroundSelectionColor);
      setForeground(textSelectColor);
    } else {
      setBackground(backgroundNonSelectionColor);
      setForeground(textNonSelectColor);
    }

    return this;
  }

}
