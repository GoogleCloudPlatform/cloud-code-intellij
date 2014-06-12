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
package com.google.gct.login.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;

/**
 * A custom cell render for {@link GoogleLoginUsersPanel#list} that manages
 * how each user item in the Google Login panel would be displayed.
 */
public class UsersListCellRenderer extends JComponent implements ListCellRenderer {
  private final Color ACTIVE_COLOR = JBColor.LIGHT_GRAY;
  private final Font PLAIN_NAME_FONT;
  private final Font PLAIN_EMAIL_FONT;
  private final Dimension MAIN_PANEL_DIMENSION;

  public UsersListCellRenderer() {
    PLAIN_NAME_FONT = new Font("Helvetica", Font.BOLD, 13);
    PLAIN_EMAIL_FONT = new Font("Helvetica", Font.PLAIN, 13);;
    MAIN_PANEL_DIMENSION = new Dimension(250, 68);
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if(!(value instanceof UsersListItem)) {
      return null;
    }

    JPanel mainPanel = new JPanel();
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    mainPanel.setPreferredSize(MAIN_PANEL_DIMENSION);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);
    // TODO: make mainPanel components left-justified

    // Update colors
    final Color bg = isSelected ? ACTIVE_COLOR : UIUtil.getListBackground();
    final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    mainPanel.setBackground(bg);
    mainPanel.setForeground(fg);


    Image imageIcon = ((UsersListItem)value).getUserPicture();
    mainPanel.add(new ImagePanel(imageIcon));
    mainPanel.add(createTextDisplay(isSelected, (UsersListItem)value));

    // TODO: add Separator to bottom of panel

    return mainPanel;
  }

  protected JComponent createTextDisplay(boolean isSelected, UsersListItem usersListItem) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(2,1));

    final Color bg = isSelected ? ACTIVE_COLOR : UIUtil.getListBackground();
    final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);

    JLabel nameLabel = new JLabel( usersListItem.getUserName());
    nameLabel.setFont(PLAIN_NAME_FONT);
    panel.add(nameLabel);

    JLabel emailLabel = new JLabel(usersListItem.getUserEmail());
    emailLabel.setFont(PLAIN_EMAIL_FONT);
    panel.add(emailLabel);

    return panel;
  }

  private class ImagePanel extends JPanel {
    private Image img;
    private final int IMAGE_STARTING_POINT_X = 10;
    private final int IMAGE_STARTING_POINT_Y = 10;
    private final Dimension PANEL_DIMENSION = new Dimension(68, 68);
    private final Dimension PLAIN_IMAGE_SIZE = new Dimension(48, 48);

    public ImagePanel(Image image) {
      img = image;
      setPreferredSize(PANEL_DIMENSION);
      setMinimumSize(PANEL_DIMENSION);
      setMaximumSize(PANEL_DIMENSION);
      setSize(PANEL_DIMENSION);
      setLayout(null);
    }

    @Override
    public void paintComponent(Graphics graphics) {
       graphics.drawImage(img, IMAGE_STARTING_POINT_X, IMAGE_STARTING_POINT_Y,
        PLAIN_IMAGE_SIZE.width, PLAIN_IMAGE_SIZE.height, null);
    }
  }
}
