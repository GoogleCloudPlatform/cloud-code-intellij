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
import com.intellij.util.ui.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.net.URL;


/**
 * A custom cell render for {@link GoogleLoginUsersPanel#list} that manages
 * how each user item in the Google Login panel would be displayed.
 */
public class UsersListCellRenderer extends JComponent implements ListCellRenderer {
  private final static String CLOUD_LABEL_TEXT = "Open Cloud Console";
  private final static String PLAY_LABEL_TEXT = "Open Play Developer Console";
  private final static String DEFAULT_AVATAR = "/icons/loginAvatar@2x.png";
  private final static String GOOGLE_IMG = "/icons/google.png";
  private final static String SIGN_IN_TEXT = "Sign in with your Google account";
  private final Color ACTIVE_COLOR = JBColor.LIGHT_GRAY;
  private final int PLAIN_USER_IMAGE_WIDTH = 48;
  private final int PLAIN_USER_IMAGE_HEIGHT = 48;
  private final int ACTIVE_USER_IMAGE_WIDTH = 96;
  private final int ACTIVE_USER_IMAGE_HEIGHT = 96;
  private final int GOOGLE_IMAGE_WIDTH = 96;
  private final int GOOGLE_IMAGE_HEIGHT = 35;
  private final int GOOGLE_IMAGE_NORTH = 18;
  private final int GOOGLE_IMAGE_WEST = 18;
  private final int WELCOME_LABEL_NORTH = 15;
  private final int WELCOME_LABEL_SOUTH = 25;
  private final int WELCOME_LABEL_EAST = 19;
  private final int WELCOME_LABEL_WEST = 21;
  private final int HGAP = 10;
  private final int VGAP = 10;
  private final Font NAME_FONT;
  private final Font GENERAL_FONT;
  private final Dimension MAIN_PANEL_DIMENSION;
  private final Dimension ACTIVE_MAIN_PANEL_DIMENSION;
  private final Dimension CLOUD_LABEL_DIMENSION;
  private final Dimension PLAY_LABEL_DIMENSION;

  public UsersListCellRenderer() {
    NAME_FONT = new Font("Helvetica", Font.BOLD, 13);
    GENERAL_FONT = new Font("Helvetica", Font.PLAIN, 13);;
    MAIN_PANEL_DIMENSION = new Dimension(250, 68);
    ACTIVE_MAIN_PANEL_DIMENSION = new Dimension(250, 116);

    FontMetrics fontMetrics = getFontMetrics(GENERAL_FONT);
    CLOUD_LABEL_DIMENSION = new Dimension(fontMetrics.stringWidth(CLOUD_LABEL_TEXT), fontMetrics.getHeight());
    PLAY_LABEL_DIMENSION = new Dimension(fontMetrics.stringWidth(PLAY_LABEL_TEXT), fontMetrics.getHeight());
  }

  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if(value instanceof NoUsersListItem) {
      return createNoUserDisplay();
    }

    if(!(value instanceof UsersListItem)) {
      return null;
    }
    UsersListItem usersListItem = (UsersListItem)value;

    boolean calcIsSelected;
    if (list.getSelectedIndex() == index) {
      calcIsSelected = true;
    } else {
      calcIsSelected = false;
    }

    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
    mainPanel.setMinimumSize(calcIsSelected ? ACTIVE_MAIN_PANEL_DIMENSION : MAIN_PANEL_DIMENSION);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);

    // Update colors
    final Color bg = calcIsSelected ? ACTIVE_COLOR : UIUtil.getListBackground();
    final Color fg = calcIsSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    mainPanel.setBackground(bg);
    mainPanel.setForeground(fg);

    // TODO: add step to cache scaled image
    Image image = usersListItem.getUserPicture();
    if(image == null){
      // use default image
      URL url = UsersListCellRenderer.class.getResource(DEFAULT_AVATAR);
      image = Toolkit.getDefaultToolkit().getImage(url);
    }

    int imageWidth = calcIsSelected ? ACTIVE_USER_IMAGE_WIDTH : PLAIN_USER_IMAGE_WIDTH;
    int imageHeight = calcIsSelected ? ACTIVE_USER_IMAGE_HEIGHT : PLAIN_USER_IMAGE_HEIGHT;
    Image scaledImage = image.getScaledInstance(imageWidth, imageHeight, java.awt.Image.SCALE_SMOOTH);

    JComponent textPanel;
    if (calcIsSelected) {
      textPanel =  createActiveTextDisplay(usersListItem);
    } else {
      textPanel =  createTextDisplay(calcIsSelected, usersListItem);
    }

    mainPanel.add(new JLabel(new ImageIcon(scaledImage)));
    mainPanel.add(textPanel);
    mainPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtil.getBorderColor()));

    return mainPanel;
  }

  public boolean inPlayConsoleUrl(Point point, int activeIndex) {
    // 2 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - PLAY_LABEL_DIMENSION.getHeight()
      - CLOUD_LABEL_DIMENSION.getHeight() - 2 + (MAIN_PANEL_DIMENSION.getHeight() * activeIndex);
    double playYEnd = playYStart + PLAY_LABEL_DIMENSION.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + PLAY_LABEL_DIMENSION.getWidth();

    if((point.getX() > playXStart) && (point.getX() < playXEnd)
       && (point.getY() > playYStart) && (point.getY() < playYEnd)) {
      return true;
    }

    return false;
  }

  public boolean inCloudConsoleUrl(Point point, int activeIndex) {
    // 3 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - CLOUD_LABEL_DIMENSION.getHeight()
      - 3 + (MAIN_PANEL_DIMENSION.getHeight() * activeIndex);
    double playYEnd = playYStart + CLOUD_LABEL_DIMENSION.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + CLOUD_LABEL_DIMENSION.getWidth();

    if((point.getX() > playXStart) && (point.getX() < playXEnd)
       && (point.getY() > playYStart) && (point.getY() < playYEnd)) {
      return true;
    }

    return false;
  }

  public int getMainPanelHeight() {
    return (int)MAIN_PANEL_DIMENSION.getHeight();
  }

  public int getActivePanelHeight() {
    return (int)ACTIVE_MAIN_PANEL_DIMENSION.getHeight();
  }

  private JComponent createTextDisplay(boolean isSelected, UsersListItem usersListItem) {
    final JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(2,1));

    final Color bg = isSelected ? ACTIVE_COLOR : UIUtil.getListBackground();
    final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);

    JLabel nameLabel = new JLabel( usersListItem.getUserName());
    nameLabel.setFont(NAME_FONT);
    panel.add(nameLabel);

    JLabel emailLabel = new JLabel(usersListItem.getUserEmail());
    emailLabel.setFont(GENERAL_FONT);
    panel.add(emailLabel);

    return panel;
  }

  private JComponent createActiveTextDisplay(UsersListItem usersListItem) {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new GridBagLayout());

    mainPanel.setBackground(ACTIVE_COLOR);
    mainPanel.setForeground(UIUtil.getListSelectionForeground());
    mainPanel.setPreferredSize(new Dimension(200, 96));

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
    bottomPanel.setBackground(ACTIVE_COLOR);
    bottomPanel.setForeground(UIUtil.getListSelectionForeground());
    bottomPanel.setPreferredSize(new Dimension(200, 40));

    JLabel playLabel = new JLabel(PLAY_LABEL_TEXT);
    playLabel.setFont(GENERAL_FONT);
    playLabel.setForeground(JBColor.BLUE);
    playLabel.setPreferredSize(PLAY_LABEL_DIMENSION);
    bottomPanel.add(playLabel, BOTTOM_ALIGNMENT);

    JLabel cloudLabel = new JLabel(CLOUD_LABEL_TEXT);
    cloudLabel.setFont(GENERAL_FONT);
    cloudLabel.setForeground(JBColor.BLUE);
    cloudLabel.setPreferredSize(CLOUD_LABEL_DIMENSION);
    bottomPanel.add(cloudLabel, BOTTOM_ALIGNMENT);

    GridBagConstraints topConstraints = new GridBagConstraints();
    topConstraints.gridx = 0;
    topConstraints.gridy = 0;
    topConstraints.anchor = GridBagConstraints.NORTHWEST;

    GridBagConstraints bottomConstraints = new GridBagConstraints();
    bottomConstraints.gridx = 0;
    bottomConstraints.gridy = 1;
    bottomConstraints.weightx = 1;
    bottomConstraints.weighty = 5;
    bottomConstraints.anchor = GridBagConstraints.SOUTHWEST;

    JComponent topPanel = createTextDisplay(true, usersListItem);
    mainPanel.add(topPanel, topConstraints);
    mainPanel.add(bottomPanel, bottomConstraints);
    return mainPanel;
  }

  private JPanel createNoUserDisplay() {
    JPanel mainPanel = new JPanel();
    BoxLayout layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
    mainPanel.setLayout(layout);
    mainPanel.setBackground(JBColor.WHITE);

    URL url = UsersListCellRenderer.class.getResource(GOOGLE_IMG);
    Image image = Toolkit.getDefaultToolkit().getImage(url);
    Image scaledImage = image.getScaledInstance(
      GOOGLE_IMAGE_WIDTH, GOOGLE_IMAGE_HEIGHT, java.awt.Image.SCALE_SMOOTH);
    JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));

    JLabel textLabel = new JLabel(SIGN_IN_TEXT);
    Dimension textSize = textLabel.getPreferredSize();
    textLabel.setPreferredSize(new Dimension((int)textSize.getWidth() + WELCOME_LABEL_EAST,
      (int)textSize.getHeight()));

    mainPanel.add(Box.createVerticalStrut(GOOGLE_IMAGE_NORTH));
    mainPanel.add(Box.createHorizontalStrut(GOOGLE_IMAGE_WEST));
    mainPanel.add(imageLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_NORTH));
    mainPanel.add(Box.createHorizontalStrut(WELCOME_LABEL_WEST - GOOGLE_IMAGE_WEST));
    mainPanel.add(textLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_SOUTH));

    return mainPanel;
  }
}
