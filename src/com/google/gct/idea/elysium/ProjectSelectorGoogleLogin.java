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
package com.google.gct.idea.elysium;

import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import icons.GoogleCloudToolsIcons;
import icons.GoogleLoginIcons;

import javax.swing.*;
import java.awt.*;

/**
 * UI for the node that prompts for signin.
 */
class ProjectSelectorGoogleLogin extends JPanel {
  public static final int PREFERRED_HEIGHT = 150;

  public ProjectSelectorGoogleLogin() {
    this.setLayout(new GridBagLayout());
    this.setPreferredSize(new Dimension(ProjectSelector.MIN_WIDTH, PREFERRED_HEIGHT));
    this.setOpaque(false);

    JLabel googleIcon = new JBLabel();

    setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
    googleIcon.setHorizontalAlignment(SwingConstants.CENTER);
    googleIcon.setVerticalAlignment(SwingConstants.CENTER);
    googleIcon.setOpaque(false);
    googleIcon.setIcon(GoogleLoginIcons.GOOGLE_LOGO);
    googleIcon.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 0;
    this.add(googleIcon, c);

    JTextArea signinText = new JTextArea();
    signinText.setFont(UIUtil.getLabelFont());
    signinText.setLineWrap(true);
    signinText.setWrapStyleWord(true);
    signinText.setOpaque(false);
    signinText.setText("Sign in to Android Studio with your Google account to list your Google Developers Console projects.");
    c.gridx = 0;
    c.gridy = 1;
    c.weighty = 1;
    c.gridwidth = 2;
    c.weightx = 1;
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints.CENTER;
    this.add(signinText, c);
  }
}
