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

import com.intellij.util.ui.UIUtil;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Created by eshaul on 1/4/17.
 */
public class AddRepositoryNotificationPanel extends JPanel {

  public static final int PREFERRED_HEIGHT = 150;
  public static final int MIN_WIDTH = 450;

  public AddRepositoryNotificationPanel() {
    setLayout(new GridBagLayout());
//    setPreferredSize(new Dimension(MIN_WIDTH, PREFERRED_HEIGHT));
    setOpaque(false);


    setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weighty = 0;

    JTextArea notificationTextArea = new JTextArea();
    notificationTextArea.setFont(UIUtil.getLabelFont());
    notificationTextArea.setLineWrap(false);
    notificationTextArea.setWrapStyleWord(true);
    notificationTextArea.setOpaque(false);
    notificationTextArea.setText("You ain't got no repos");
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.gridwidth = 2;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.anchor = GridBagConstraints.CENTER;
    add(notificationTextArea, constraints);
  }

}
