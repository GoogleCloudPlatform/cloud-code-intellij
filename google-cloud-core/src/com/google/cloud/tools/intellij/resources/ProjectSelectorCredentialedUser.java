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

import com.intellij.ui.components.JBLabel;
import com.intellij.util.containers.hash.HashMap;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.jetbrains.annotations.Nullable;

/**
 * UI that represents a single Google Login It displays an image of the user with his or her email.
 */
class ProjectSelectorCredentialedUser extends JPanel {

  private JLabel userIcon = new JBLabel();
  private JLabel name = new JBLabel();
  private JLabel emailLabel = new JBLabel();
  // We use an image cache because multiple image creates causes a performance hit.
  private Map<Image, Icon> imageCache = new HashMap<Image, Icon>();

  public ProjectSelectorCredentialedUser() {
    setLayout(new GridBagLayout());

    this.setOpaque(false);
    this.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

    userIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
    userIcon.setOpaque(false);
    userIcon.setHorizontalAlignment(SwingConstants.CENTER);
    userIcon.setVerticalAlignment(SwingConstants.CENTER);

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridheight = 3;
    constraints.weightx = 0;
    constraints.weighty = 0;
    add(userIcon, constraints);

    Font originalFont = name.getFont();
    name.setOpaque(false);
    Font boldFont = new Font(originalFont.getFontName(), Font.BOLD, originalFont.getSize() + 1);
    name.setFont(boldFont);

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.gridheight = 1;
    constraints.weightx = 1;
    constraints.weighty = 0;
    add(name, constraints);

    emailLabel.setOpaque(false);
    Font plainFont = new Font(originalFont.getFontName(), Font.ITALIC, originalFont.getSize() - 1);
    emailLabel.setFont(plainFont);

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 1;
    constraints.gridy = 1;
    constraints.gridheight = 1;
    constraints.weightx = 1;
    constraints.weighty = 0;
    add(emailLabel, constraints);
  }

  public void initialize(@Nullable Image image, @Nullable String userName, @Nullable String email) {
    Icon scaledIcon;
    if (image == null) {
      scaledIcon = null;
    } else if (!imageCache.containsKey(image)) {
      scaledIcon = new ImageIcon(image.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
      imageCache.put(image, scaledIcon);
    } else {
      scaledIcon = imageCache.get(image);
    }

    userIcon.setIcon(scaledIcon);
    name.setText(userName);
    emailLabel.setText(email);

    this.setPreferredSize(
        new Dimension(
            userIcon.getPreferredSize().width
                + name.getPreferredSize().width
                + emailLabel.getPreferredSize().width,
            Math.max(
                scaledIcon != null ? scaledIcon.getIconHeight() + 2 : 0,
                emailLabel.getPreferredSize().height + name.getPreferredSize().height + 4)));
  }
}
