/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.cloudlogging;

import com.intellij.ui.components.JBLabel;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import static com.intellij.util.ui.UIUtil.getBoundsColor;

/**
 * To animate the tree style logs in App Engine logs
 * Created by amulyau on 6/9/15.
 */
public class LogsTreeCellRenderer extends DefaultTreeCellRenderer {

  /**Color of the background when object is not selected*/
  private final Color backgroundNonSelectionColor = getBackgroundNonSelectionColor();
  /**Color of the background when object is selected*/
  private final Color  backgroundSelectionColor = getBackgroundSelectionColor();
  /**Color of the text when object is not selected*/
  private final Color textNonSelectColor = getTextNonSelectionColor();
  /**Color of the text when object is selected*/
  private final Color textSelectColor = getTextSelectionColor();
  /**The view that contains the tree that has the logs*/
  private final AppEngineLogToolWindowView view;

  /**
   * Constructor for the renderer that takes in the view
   * @param view The App Engine Log Tool Window view that provides the font size.
   */
  public LogsTreeCellRenderer(AppEngineLogToolWindowView view) {

    this.view = view;
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                boolean expanded, boolean leaf, int row,
                                                boolean hasFocus) {

    TextAreaNode treeNode = (TextAreaNode)value;
    String text = treeNode.getText();
    PanelExtend panel = new PanelExtend();
    JBLabel iconLabel = panel.getLabelIcon();
    JTextArea textArea = panel.getLogText();

    float fontSize;
    fontSize = view.getFontSize();
    textArea.setText(text);
    textArea.setFont(textArea.getFont().deriveFont(fontSize));

    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setOpaque(false);

    if (treeNode.getIcon() == null) {
      textArea.setBorder(BorderFactory.createEmptyBorder());
    } else if ((treeNode.isLeaf()) && (!treeNode.isRoot()) && (!((TextAreaNode) treeNode
        .getParent()).isRoot())) {
      textArea.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, getBoundsColor()));
    } else {
      textArea.setBorder(BorderFactory.createEmptyBorder());
    }

    if (selected) {
      panel.setBackground(backgroundSelectionColor);
      textArea.setForeground(textSelectColor);

    } else {
      panel.setBackground(backgroundNonSelectionColor);
      textArea.setForeground(textNonSelectColor);
    }

    iconLabel.setIcon(treeNode.getIcon());
    panel.setLabelIcon(iconLabel);
    panel.setTextArea(textArea);

    return panel;
  }

}

