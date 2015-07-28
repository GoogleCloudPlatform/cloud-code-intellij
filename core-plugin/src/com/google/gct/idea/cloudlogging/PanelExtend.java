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

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;

import java.awt.*;

import javax.swing.*;

/**
 * Panel Extend is what goes in each JTree node
 * Created by amulyau on 6/24/15.
 */
public class PanelExtend extends JBPanel {

  private final GridBagConstraints gb;
  private JTextArea log = null;
  private JBLabel labelIcon;

  /**
   * Constructor to properly create the JBPanel and the components for it
   * */
  public PanelExtend() {
    gb = new GridBagConstraints();
    this.setLayout(new GridBagLayout());
    log = new JTextArea();
    labelIcon = new JBLabel();
    this.setBackground(JBColor.white);
  }

  /**
   * Gets the Log text area
   * @return JTextArea of the log
   */
  public JTextArea getLogText() {
    return log;
  }

  /**
   * Gets the label used to hold the icon
   * @return JLabel for the icon
   */
  public JBLabel getLabelIcon() {
    return labelIcon;
  }

  /**
   * Sets the label icon in proper place in panel
   * @param label Label to set in panel
   */
  public void setLabelIcon(JBLabel label) {
    this.labelIcon = label;

    gb.gridx = 0;
    gb.gridy = 0;
    gb.weightx = 0.0;
    gb.weighty = 0.0;
    gb.gridheight = 1;
    gb.insets = new Insets(0, 5, 0, 5);
    gb.fill = GridBagConstraints.BOTH;
    gb.gridwidth = 1;
    gb.anchor = GridBagConstraints.FIRST_LINE_START; //top left corner always
    this.add(labelIcon,gb);
  }

  /**
   * Sets the text area in correct place in panel
   * @param log Text area to set in panel
   */
  public void setTextArea(JTextArea log) {
    this.log = log;

    gb.gridx = 1;
    gb.gridy = 0;
    gb.weightx = 1.0;
    gb.weighty = 2.0;
    gb.gridheight = 2;
    gb.insets = new Insets(0, 0, 0, 0);
    gb.fill = GridBagConstraints.BOTH;
    gb.gridwidth = GridBagConstraints.REMAINDER;
    gb.anchor = GridBagConstraints.CENTER; //top left corner always
    this.add(log,gb);
  }

}
