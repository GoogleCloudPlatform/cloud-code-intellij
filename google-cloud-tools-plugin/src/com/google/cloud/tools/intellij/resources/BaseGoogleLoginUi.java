/*
 * Copyright (C) 2016 The Android Open Source Project
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

import com.google.cloud.tools.intellij.login.ui.GoogleLoginIcons;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.NotNull;

/** UI for the node that prompts for Google Login. */
public class BaseGoogleLoginUi extends JPanel {

  public static final int PREFERRED_HEIGHT = 150;
  public static final int MIN_WIDTH = 450;

  public BaseGoogleLoginUi(@NotNull String signinText) {
    setLayout(new GridBagLayout());
    setPreferredSize(new Dimension(MIN_WIDTH, PREFERRED_HEIGHT));
    setOpaque(false);

    JLabel googleIcon = new JBLabel();

    setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
    googleIcon.setHorizontalAlignment(SwingConstants.CENTER);
    googleIcon.setVerticalAlignment(SwingConstants.CENTER);
    googleIcon.setOpaque(false);
    googleIcon.setIcon(GoogleLoginIcons.GOOGLE_LOGO);
    googleIcon.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weighty = 0;
    add(googleIcon, constraints);

    JTextArea signinTextArea = new JTextArea();
    signinTextArea.setFont(UIUtil.getLabelFont());
    signinTextArea.setLineWrap(true);
    signinTextArea.setWrapStyleWord(true);
    signinTextArea.setOpaque(false);
    signinTextArea.setText(signinText);
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.gridwidth = 2;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    add(signinTextArea, constraints);
  }
}
