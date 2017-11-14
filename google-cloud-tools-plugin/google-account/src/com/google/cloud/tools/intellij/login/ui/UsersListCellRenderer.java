/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.cloud.tools.intellij.login.ui;

import com.google.api.client.util.Maps;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.util.AccountMessageBundle;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import org.jetbrains.annotations.Nullable;

/**
 * A custom cell render for {@link GoogleLoginUsersPanel#list} that manages how each user item in
 * the Google Login panel would be displayed.
 */
public class UsersListCellRenderer extends JComponent implements ListCellRenderer<UsersListItem> {

  private static final String CLOUD_LABEL_TEXT = AccountMessageBundle
      .message("login.panel.play.console.link.text");
  private static final String PLAY_LABEL_TEXT = AccountMessageBundle
      .message("login.panel.cloud.console.link.text");
  private static final String DEFAULT_AVATAR = "/icons/loginAvatar@2x.png";
  private static final String SIGN_IN_TEXT = AccountMessageBundle
      .message("login.panel.sing.in.body.html");
  private static final String LEARN_MORE_TEXT = AccountMessageBundle
      .message("login.panel.learn.more.link.text");
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
  private final Color activeColor;
  private final Color inactiveColor;
  private final int generalFontHeight;
  private final Font nameFont;
  private final Font generalFont;
  private final Dimension mainPanelDimension;
  private final Dimension activeMainPanelDimension;
  private final Dimension cloudLabelDimension;
  private final Dimension playLabelDimension;
  private final Dimension learnMoreLabelDimension;
  private JLabel googleImageLabel;

  /**
   * Maps user emails to large user image icons.
   */
  private final Map<String, Image> userLargeImageCache = Maps.newHashMap();
  /**
   * Maps user emails to small user image icons.
   */
  private final Map<String, Image> userSmallImageCache = Maps.newHashMap();

  /**
   * Initializes the custom cell renderer.
   */
  public UsersListCellRenderer() {
    nameFont = new Font("Helvetica", Font.BOLD, 13);
    generalFont = new Font("Helvetica", Font.PLAIN, 13);
    mainPanelDimension = new Dimension(250, 68);
    activeMainPanelDimension = new Dimension(250, 116);
    activeColor =
        UIUtil.isUnderDarcula() ? UIManager.getColor("TextField.background") : Color.WHITE;
    inactiveColor = UIUtil.isUnderDarcula() ? UIManager.getColor("darcula.inactiveBackground")
        : new Color(0xf5f5f5);

    FontMetrics fontMetrics = getFontMetrics(generalFont);
    generalFontHeight = fontMetrics.getHeight();
    cloudLabelDimension = new Dimension(fontMetrics.stringWidth(CLOUD_LABEL_TEXT),
        generalFontHeight);
    playLabelDimension = new Dimension(fontMetrics.stringWidth(PLAY_LABEL_TEXT), generalFontHeight);
    learnMoreLabelDimension = new Dimension(fontMetrics.stringWidth(LEARN_MORE_TEXT),
        generalFontHeight);
  }

  @Nullable
  @Override
  public Component getListCellRendererComponent(
      JList<? extends UsersListItem> list,
      UsersListItem usersListItem,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    if (usersListItem instanceof NoUsersListItem) {
      return createNoUserDisplay();
    }

    final CredentialedUser activeUser = Services.getLoginService().getActiveUser();
    final boolean isActiveUserSelected =
        activeUser != null && usersListItem.getUserEmail().equals(activeUser.getEmail());

    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
    mainPanel.setMinimumSize(isActiveUserSelected ? activeMainPanelDimension : mainPanelDimension);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);

    // Update colors
    final Color bg = isActiveUserSelected ? activeColor : inactiveColor;
    final Color fg =
        isActiveUserSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    mainPanel.setBackground(bg);
    mainPanel.setForeground(fg);

    Image image = usersListItem.getUserPicture();
    if (image == null) {
      // Use default profile image.
      image = Toolkit.getDefaultToolkit()
          .getImage(UsersListCellRenderer.class.getResource(DEFAULT_AVATAR));
    }

    final int imageWidth;
    final int imageHeight;
    final Map<String, Image> userImageCache;
    if (isActiveUserSelected) {
      imageWidth = ACTIVE_USER_IMAGE_WIDTH;
      imageHeight = ACTIVE_USER_IMAGE_HEIGHT;
      userImageCache = userLargeImageCache;
    } else {
      imageWidth = PLAIN_USER_IMAGE_WIDTH;
      imageHeight = PLAIN_USER_IMAGE_HEIGHT;
      userImageCache = userSmallImageCache;
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
    mainPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));

    return mainPanel;
  }

  /**
   * Determines if clicked point is on the play console url.
   */
  public boolean inPlayConsoleUrl(Point point, int activeIndex) {
    // 2 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - playLabelDimension.getHeight()
        - cloudLabelDimension.getHeight() - 2 + (mainPanelDimension.getHeight() * activeIndex)
        + USER_LABEL_VERTICAL_STRUT;
    double playYEnd = playYStart + playLabelDimension.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + playLabelDimension.getWidth();
    return (point.getX() > playXStart) && (point.getX() < playXEnd) && (point.getY() > playYStart)
        && (point.getY() < playYEnd);
  }

  /**
   * Determines if clicked point is on the cloud console url.
   */
  public boolean inCloudConsoleUrl(Point point, int activeIndex) {
    // 3 is for the number of labels before this one
    double playYStart = VGAP + ACTIVE_USER_IMAGE_HEIGHT - cloudLabelDimension.getHeight()
        - 3 + (mainPanelDimension.getHeight() * activeIndex) + (USER_LABEL_VERTICAL_STRUT * 2);
    double playYEnd = playYStart + cloudLabelDimension.getHeight();
    double playXStart = ACTIVE_USER_IMAGE_WIDTH + HGAP + VGAP;
    double playXEnd = playXStart + cloudLabelDimension.getWidth();
    return (point.getX() > playXStart) && (point.getX() < playXEnd) && (point.getY() > playYStart)
        && (point.getY() < playYEnd);
  }

  /**
   * Determines if clicked point is on the learn more url.
   */
  public boolean inLearnMoreUrl(Point point) {
    // 2 is for the number of labels and row of texts
    double urlYStart =
        GOOGLE_IMAGE_NORTH + googleImageLabel.getIcon().getIconHeight() + WELCOME_LABEL_NORTH
            + (generalFontHeight * 2) + 3;
    double urlYEnd = urlYStart + learnMoreLabelDimension.getHeight();
    double urlXStart = GOOGLE_IMAGE_WEST;
    double urlXEnd = urlXStart + learnMoreLabelDimension.getWidth();
    return (point.getX() > urlXStart) && (point.getX() < urlXEnd) && (point.getY() > urlYStart) && (
        point.getY() < urlYEnd);
  }

  public int getMainPanelHeight() {
    return (int) mainPanelDimension.getHeight();
  }

  public int getActivePanelHeight() {
    return (int) activeMainPanelDimension.getHeight();
  }

  private JComponent createTextDisplay(boolean isSelected, UsersListItem usersListItem) {
    final JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    final Color bg = isSelected ? activeColor : inactiveColor;
    final Color fg = isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getListForeground();
    panel.setBackground(bg);
    panel.setForeground(fg);

    JLabel nameLabel = new JLabel(usersListItem.getUserName());
    nameLabel.setFont(nameFont);
    panel.add(nameLabel);
    panel.add(Box.createVerticalStrut(USER_LABEL_VERTICAL_STRUT));

    JLabel emailLabel = new JLabel(usersListItem.getUserEmail());
    emailLabel.setFont(generalFont);
    panel.add(emailLabel);

    return panel;
  }

  private JComponent createActiveTextDisplay(UsersListItem usersListItem) {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new GridBagLayout());

    mainPanel.setBackground(activeColor);
    mainPanel.setForeground(UIUtil.getListSelectionForeground());
    mainPanel.setPreferredSize(new Dimension(220, ACTIVE_USER_IMAGE_HEIGHT));

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
    bottomPanel.setBackground(activeColor);
    bottomPanel.setForeground(UIUtil.getListSelectionForeground());
    bottomPanel
        .setPreferredSize(new Dimension(220, (generalFontHeight * 2) + USER_LABEL_VERTICAL_STRUT));

    JLabel playLabel = new JLabel(PLAY_LABEL_TEXT);
    playLabel.setFont(generalFont);
    playLabel.setForeground(JBColor.BLUE);
    playLabel.setPreferredSize(playLabelDimension);
    bottomPanel.add(playLabel, BOTTOM_ALIGNMENT);
    bottomPanel.add(Box.createVerticalStrut(USER_LABEL_VERTICAL_STRUT));

    JLabel cloudLabel = new JLabel(CLOUD_LABEL_TEXT);
    cloudLabel.setFont(generalFont);
    cloudLabel.setForeground(JBColor.BLUE);
    cloudLabel.setPreferredSize(cloudLabelDimension);
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

    googleImageLabel = new JLabel(GoogleLoginIcons.GOOGLE_LOGO);

    JLabel signInLabel = new JLabel(SIGN_IN_TEXT);
    signInLabel.setFont(generalFont);
    Dimension textSize = signInLabel.getPreferredSize();
    signInLabel.setPreferredSize(
        new Dimension((int) textSize.getWidth() + WELCOME_LABEL_EAST, (int) textSize.getHeight()));

    JLabel urlLabel = new JLabel(LEARN_MORE_TEXT);
    urlLabel.setFont(generalFont);
    urlLabel.setForeground(JBColor.BLUE);
    urlLabel.setPreferredSize(learnMoreLabelDimension);

    mainPanel.add(Box.createVerticalStrut(GOOGLE_IMAGE_NORTH));
    mainPanel.add(googleImageLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_NORTH));
    mainPanel.add(signInLabel);
    mainPanel.add(urlLabel);
    mainPanel.add(Box.createVerticalStrut(WELCOME_LABEL_SOUTH));

    return mainPanel;
  }
}
