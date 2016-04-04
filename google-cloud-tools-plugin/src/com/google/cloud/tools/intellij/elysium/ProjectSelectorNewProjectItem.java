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
package com.google.cloud.tools.intellij.elysium;

import com.google.cloud.tools.intellij.stats.UsageTrackerProvider;
import com.google.cloud.tools.intellij.util.GctTracking;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.UI;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * UI for the "click here to add a project" node.
 */
class ProjectSelectorNewProjectItem extends JPanel implements MouseListener, MouseInputListener {
  private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
  private static final Cursor NORMAL_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

  private JLabel clickHere;
  private JTree tree;
  private JPanel panel1;

  public ProjectSelectorNewProjectItem(@NotNull JTree tree) {
    this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));

    this.tree = tree;
    this.setOpaque(false);
    setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));

    clickHere = new JBLabel();
    clickHere.setHorizontalAlignment(SwingConstants.LEFT);
    clickHere.setForeground(UI.getColor("link.foreground"));
    clickHere.setText("<HTML><U>Click here</U></HTML>");
    clickHere.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

    add(clickHere);

    JLabel continuation = new JBLabel();
    continuation.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    continuation.setHorizontalAlignment(SwingConstants.LEFT);
    continuation.setText(" to create a new Google Developers Console project.");

    add(continuation);
  }

  private boolean isOverLink(int x, int y) {
    return x <= clickHere.getPreferredSize().width + 15;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (isOverLink(e.getX(), e.getY())) {
      TreeModel model = tree.getModel();
      if (model instanceof ProjectSelector.SelectorTreeModel) {
        ((ProjectSelector.SelectorTreeModel)model).setModelNeedsRefresh(true);
      }
      BrowserUtil.browse("https://console.developers.google.com/project");
      UsageTrackerProvider.getInstance().trackEvent(GctTracking.CATEGORY, GctTracking.PROJECT_SELECTION,
          "create.new.project", null);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
  }

  @Override
  public void mouseEntered(MouseEvent e) {
  }

  @Override
  public void mouseExited(MouseEvent e) {
  }

  @Override
  public void mouseDragged(MouseEvent e) {
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (isOverLink(e.getX(), e.getY())) {
      tree.setCursor(HAND_CURSOR);
    }
    else {
      tree.setCursor(NORMAL_CURSOR);
    }
  }
}
