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

import com.google.api.client.util.Maps;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import icons.GoogleLoginIcons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Map;

/**
 * A custom cell render for {@link GoogleLoginUsersPanel#list} that manages
 * how each user item in the Google Login panel would be displayed.
 */
public class UsersListCellRenderer extends JComponent implements ListCellRenderer {
  private final static String CLOUD_LABEL_TEXT = "Open Google Developers Console";
  private final static String PLAY_LABEL_TEXT = "Open Play Developer Console";
  private final static String DEFAULT_AVATAR = "/icons/loginAvatar@2x.png";
  private final static String SIGN_IN_TEXT = "<HTML> Sign in with your Google account to start <br> adding "
    + "Cloud functionality to your <br> Android applications from Android Studio. </HTML>";
  private static final String LEARN_MORE_TEXT = "Learn more";
  private static final int PLAIN_USER_IMAGE_WIDTH = 48;
  private static final int PLAIN_USER_IMAGE_HEIGHT = 48;
  private static final int ACTIVE_USER_IMAGE_WIDTH = 96;
  private static final int ACTIVE_USER_IMAGE_HEIGHT = 96;
  private static final int GOOGLE_IMAGE_NORTH = 18;
  private static final int GOOGLE_IMAGE_WEST = 18;
  private static final int WELCOME_LABEL_NORTH = 15;
  private static final int WELCOME_LABEL_SOUTH = 25;
  private static final int WELCOME_LABEL_EAST = 38;
  private static final int USER_LABEL_VERTICAL_STRUT = 3;
  private static final int HGAP = 10;
  private static final int VGAP = 10;
  private final Color myActiveColor;
  private final Color myInactiveColor;
  private final int myGeneralFontHeight;
  private final Font myNameFont;
  private final Font myGeneralFont;
  private final Dimension myMainPanelDimension;
  private final Dimension myActiveMainPanelDimension;
  private final Dimension myCloudLabelDimension;
  private final Dimension myPlayLabelDimension;
  private final Dimension myLearnMoreLabelDimension;
  private JLabel myGoogleImageLabel;

  /** Maps user emails to large user image icons. */
  private final Map<String, Image> myUserLargeImageCache = Maps.newHashMap();
  /** Maps user emails to small user image icons. */
  private final Map<String, Image> myUserSmallImageCache = Maps.newHashMap();

  public UsersListCellRenderer() {
    myNameFont = new Font("Helvetica", Font.BOLD, 13);
    myGeneralFont = new Font("Helvetica", Font.PLAIN, 13);
    myMainPanelDimension = new Dimension(250, 68);
    myActiveMainPanelDimension = new Dimension(250, 116);
    myActiveColor = UIUtil.isUnderDarcula() ? UIManager.getColor("TextField.background") : Color.WHITE;
    myInactiveColor = UIUtil.isUnderDarcula() ? UIManager.getColor("darcula.inactiveBackground") : new Color(0xf5f5f5);

    FontMetrics fontMetrics = getFontMetrics(myGeneralFont);
    myGeneralFontHeight = fontMetrics.getHeight();
    myCloudLabelDimension = new Dimension(fontMetrics.stringWidth(CLOUD_LABEL_TEXT), myGeneralFontHeight);
    myPlayLabelDimension = new Dimension(fontMetrics.stringWidth(PLAY_LABEL_TEXT), myGeneralFontHeight);
    myLearnMoreLabelDimension = new Dimension(fontMetrics.stringWidth(LEARN_MORE_TEXT), myGeneralFontHeight);
  }

  @Nullable
  @Override
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    if (value instanceof NoUsersListItem) {
      return createNoUserDisplay();
    }
    if (!(value instanceof UsersListItem)) {
      return null;
    }

    final UsersListItem usersListItem = (UsersListItem)value;
    final CredentialedUser activeUser = GoogleLogin.getInstance().getActiveUser();
    final boolean isActiveUserSelected = activeUser != null && usersListItem.getUserEmail().equals(activeUser.getEmail());

    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
    mainPanel.setMinimumSize(isActiveUserSelected ? myActiveMainPanelDimension : myMainPanelDimension);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);

    // Update colors
    final Color bg = isActiveUserSelected ? myActiveColor : myInactiveColor;
    final Color fg = isActiveUserSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    mainPanel.setBackground(bg);
    mainPanel.setForeground(fg);

    Image image = usersListItem.getUserPicture();
    if (image == null){
      // Use default profile image.
      image = Toolkit.getDefaultToolkit().getImage(UsersListCellRenderer.class.getResource(DEFAULT_AVATAR));
    }

    final int imageWidth, imageHeight;
    final Map<String, Image> userImageCache;
    if (isActiveUserSelected) {
      imageWidth = ACTIVE_USER_IMAGE_WIDTH;
      imageHeight = ACTIVE_USER_IMAGE_HEIGHT;
      userImageCache = myUserLargeImageCache;
    } else {
      imageWidth = PLAIN_USER_IMAGE_WIDTH;
      imageHeight = PLAIN_USER_IMAGE_HEIGHT;
      userImageCache = myUserSmallImageCache;
    }

    final Image scaledImage;
    if (!userImageCache.containsKey(usersListItem.getUserEmail())) {
      scaledImage = image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
      userImageCache.put(usersListItem.getUserEmail(), scaledImage);
    } else {
      scaledImage = userImageCache.get(usersListItem.getUserEmail());
    }

    final JComponent textPanel;
    if (isActiveUserSelected) {
      textPanel = createActiveTextDisplay(usersListItem);
    } else {
      textPanel = createTextDisplay(false, usersListItem);
    }

    mainPanel.add(new JLabel(new ImageIcon(scaledImage)));
    mainPanel.add(textPanel);
    mainPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtil.getBorderColor()));

    return mainPanel;
  }

  public boolean inPlayConsoleUrl(Point point, int activeIndex) {
    // 2 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - myPlayLabelDimension.getHeight()
      - myCloudLabelDimension.getHeight() - 2 + (myMainPanelDimension.getHeight() * activeIndex)
      + USER_LABEL_VERTICAL_STRUT;
    double playYEnd = playYStart + myPlayLabelDimension.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + myPlayLabelDimension.getWidth();
    return (point.getX() > playXStart) && (point.getX() < playXEnd) && (point.getY() > playYStart) && (point.getY() < playYEnd);
  }

  public boolean inCloudConsoleUrl(Point point, int activeIndex) {
    // 3 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - myCloudLabelDimension.getHeight()
      - 3 + (myMainPanelDimension.getHeight() * activeIndex) + (USER_LABEL_VERTICAL_STRUT * 2);
    double playYEnd = playYStart + myCloudLabelDimension.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + myCloudLabelDimension.getWidth();
    return (point.getX() > playXStart) && (point.getX() < playXEnd) && (point.getY() > playYStart) && (point.getY() < playYEnd);
  }

  public boolean inLearnMoreUrl(Point point) {
    // 3 is for the number of labels and row of texts
    double urlYStart = GOOGLE_IMAGE_NORTH + myGoogleImageLabel.getIcon().getIconHeight() + WELCOME_LABEL_NORTH
      + (myGeneralFontHeight * 3) + 3;
    double urlYEnd = urlYStart + myLearnMoreLabelDimension.getHeight();
    double urlXStart = GOOGLE_IMAGE_WEST;
    double urlXEnd = urlXStart + myLearnMoreLabelDimension.getWidth();
    return (point.getX() > urlXStart) && (point.getX() < urlXEnd) && (point.getY() > urlYStart) && (point.getY() < urlYEnd);
  }

  public int getMainPanelHeight() {
    return (int)myMainPanelDimension.getHeight();
  }

  public int getActivePanelHeight() {
    return (int)myActiveMainPanelDimension.getHeight();
  }

  private JComponent createTextDisplay(boolean isSelected, UsersListItem usersListItem) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    final Color bg = isSelected ? myActiveColor : myInactiveColor;
    final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);

    JLabel nameLabel = new JLabel( usersListItem.getUserName());
    nameLabel.setFont(myNameFont);
    panel.add(nameLabel);
    panel.add(Box.createVerticalStrut(USER_LABEL_VERTICAL_STRUT));

    JLabel emailLabel = new JLabel(usersListItem.getUserEmail());
    emailLabel.setFont(myGeneralFont);
    panel.add(emailLabel);

    return panel;
  }

  private JComponent createActiveTextDisplay(UsersListItem usersListItem) {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new GridBagLayout());

    mainPanel.setBackground(myActiveColor);
    mainPanel.setForeground(UIUtil.getListSelectionForeground());
    mainPanel.setPreferredSize(new Dimension(220, ACTIVE_USER_IMAGE_HEIGHT));

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
    bottomPanel.setBackground(myActiveColor);
    bottomPanel.setForeground(UIUtil.getListSelectionForeground());
    bottomPanel.setPreferredSize(new Dimension(220, (myGeneralFontHeight * 2) + USER_LABEL_VERTICAL_STRUT));

    JLabel playLabel = new JLabel(PLAY_LABEL_TEXT);
    playLabel.setFont(myGeneralFont);
    playLabel.setForeground(JBColor.BLUE);
    playLabel.setPreferredSize(myPlayLabelDimension);
    bottomPanel.add(playLabel, BOTTOM_ALIGNMENT);
    bottomPanel.add(Box.createVerticalStrut(USER_LABEL_VERTICAL_STRUT));

    JLabel cloudLabel = new JLabel(CLOUD_LABEL_TEXT);
    cloudLabel.setFont(myGeneralFont);
    cloudLabel.setForeground(JBColor.BLUE);
    cloudLabel.setPreferredSize(myCloudLabelDimension);
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
    mainPanel.setBorder(BorderFactory.createEmptyBorder(0, GOOGLE_IMAGE_WEST, 0, 0));

    myGoogleImageLabel = new JLabel(GoogleLoginIcons.GOOGLE_LOGO);

    JLabel signInLabel = new JLabel(SIGN_IN_TEXT);
    signInLabel.setFont(myGeneralFont);
    Dimension textSize = signInLabel.getPreferredSize();
    signInLabel.setPreferredSize(new Dimension((int)textSize.getWidth() + WELCOME_LABEL_EAST, (int)textSize.getHeight()));

    JLabel urlLabel = new JLabel(LEARN_MORE_TEXT);
    urlLabel.setFont(myGeneralFont);
    urlLabel.setForeground(JBColor.BLUE);
    urlLabel.setPreferredSize(myLearnMoreLabelDimension);

    mainPanel.add(Box.createVerticalStrut(GOOGLE_IMAGE_NORTH));
    mainPanel.add(myGoogleImageLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_NORTH));
    mainPanel.add(signInLabel);
    mainPanel.add(urlLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_SOUTH));

    return mainPanel;
  }
}
