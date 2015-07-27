package com.google.gct.idea.cloudlogging;/*
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

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;

import java.awt.*;

import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * Combo Box Editor to allow a ComboBox element to have a panel and label
 * Created by amulyau on 7/1/15.
 */
public class LogLevelComboBoxEditor extends BasicComboBoxEditor {

  private final Color backgroundNonSelectionColor = UIUtil.getListBackground(false); //white
  private final Color backgroundSelectionColor = UIUtil.getListBackground(true); //blue

  private final Color textNonSelectColor = UIUtil.getListForeground(false); //black
  private final Color textSelectColor = UIUtil.getListForeground(true); //white

  /**
   * Every element in the combo box for log levels is a panel
   */
  //private final JBPanel panel = new JBPanel();
  private final JBLabel label = new JBLabel();

  /**
   * The panel contains a label so constructor adds the label
   */
  public LogLevelComboBoxEditor() {

    //JBLabel label = new JBLabel();
  //  label.setOpaque(false);
  //  panel.add(label);
  }

  /**
   * Gets the panel
   * @return Panel
   */
  @Override
  public Component getEditorComponent() {

    //return this.panel;
    return this.label;
  }

}
