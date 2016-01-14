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
import com.intellij.util.containers.hash.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * UI that represents a single Google Login
 * It displays an image of the user with his or her email.
 */
class ProjectSelectorCredentialedUser extends JPanel {
  private JLabel myUserIcon = new JBLabel();
  private JLabel myName = new JBLabel();
  private JLabel myEmailLabel = new JBLabel();
  // We use an image cache because multiple image creates causes a performance hit.
  private Map<Image, Icon> myImageCache = new HashMap<Image, Icon>();

  public ProjectSelectorCredentialedUser() {
    setLayout(new GridBagLayout());

    this.setOpaque(false);
    this.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

    GridBagConstraints c = new GridBagConstraints();

    myUserIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
    myUserIcon.setOpaque(false);
    myUserIcon.setHorizontalAlignment(SwingConstants.CENTER);
    myUserIcon.setVerticalAlignment(SwingConstants.CENTER);
    c.gridx = 0;
    c.gridy = 0;
    c.gridheight = 3;
    c.weightx = 0;
    c.weighty = 0;
    add(myUserIcon, c);

    Font originalFont = myName.getFont();
    myName.setOpaque(false);
    Font boldFont = new Font(originalFont.getFontName(), Font.BOLD, originalFont.getSize() + 1);
    myName.setFont(boldFont);

    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 0;
    c.gridheight = 1;
    c.weightx = 1;
    c.weighty = 0;
    add(myName, c);

    myEmailLabel.setOpaque(false);
    Font plainFont = new Font(originalFont.getFontName(), Font.ITALIC, originalFont.getSize() - 1);
    myEmailLabel.setFont(plainFont);

    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 1;
    c.gridheight = 1;
    c.weightx = 1;
    c.weighty = 0;
    add(myEmailLabel, c);
  }

  public void initialize(@Nullable Image image, @Nullable String userName, @Nullable String email) {
    Icon scaledIcon;
    if (image == null) {
      scaledIcon = null;
    }
    else if (!myImageCache.containsKey(image)) {
      scaledIcon = new ImageIcon(image.getScaledInstance(32, 32, Image.SCALE_SMOOTH));
      myImageCache.put(image, scaledIcon);
    }
    else {
      scaledIcon = myImageCache.get(image);
    }

    myUserIcon.setIcon(scaledIcon);
    myName.setText(userName);
    myEmailLabel.setText(email);

    this.setPreferredSize(
      new Dimension(myUserIcon.getPreferredSize().width + myName.getPreferredSize().width + myEmailLabel.getPreferredSize().width,
                    Math.max(scaledIcon != null ? scaledIcon.getIconHeight() + 2 : 0, myEmailLabel.getPreferredSize().height +
                                                             myName.getPreferredSize().height + 4)));
  }
}
