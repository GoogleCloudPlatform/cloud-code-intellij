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

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeCellRenderer;

/**
 * UI for Tree Nodes.
 * This is mainly to change Tree Node's Text Area size to properly fit all data
 * Created by amulyau on 6/23/15.
 */
public class BasicWideNodeTreeUI extends BasicTreeUI {

  private JTree tree;
  private final AppEngineLogToolWindowView view;
  /**Width of insets on icons to give padding*/
  private final int iconInsets = 10;
  /**Vertical height to add to each node to make more readable*/
  private final int addHeight = 5;

  /**
   * Constructor
   * @param appEngineLogToolWindowView View that controls the visual components of the plugin
   */
  public BasicWideNodeTreeUI(AppEngineLogToolWindowView appEngineLogToolWindowView) {
    super();
    view = appEngineLogToolWindowView;
  }

  /**
   * As long as the component isnot null, it installs the UI to the tree
   * @param c JTree Component passed in
   */
  @Override
  public void installUI(JComponent c) {
    if (c != null) {
      tree = (JTree)c;
      super.installUI(c);
    }
  }

  /**
   * Returns the Cell Renderer
   * @return LogsTreeCellRenderer is a custom renderer
   */
  @Override
  protected TreeCellRenderer createDefaultCellRenderer() {
    return new LogsTreeCellRenderer(view);
  }

  /**
   * Returns a new Node Demensions Handler
   * @return Node Dimentions Handler
   */
  @Override
  protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
    return new NodeDimensionsHandler();
  }

  /**
   * Gets each node in tree and sets its dimensions properly
   */
  private class NodeDimensionsHandler extends AbstractLayoutCache.NodeDimensions {
    @Override
    public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded,
                                       Rectangle size) {
      if (currentCellRenderer != null) {
        Component comp;
        comp = currentCellRenderer.getTreeCellRendererComponent(tree, value,
            tree.isRowSelected(row), expanded,treeModel.isLeaf(value), row, false);
        if (tree != null) {
          rendererPane.add(comp);
          comp.validate();
        }

        JTextArea textArea = ((PanelExtend)comp).getLogText();
        Icon icon = ((PanelExtend)comp).getLabelIcon().getIcon();
        Dimension preferredSize = comp.getPreferredSize();

        if (size != null) {
          size.x = getRowX(row, depth);
          Dimension dim = getTextDimensions(tree.getParent().getWidth(), size.x, textArea, icon,
              view.getTextWrap());

          size.width = ((int)dim.getWidth());
          size.height = ((int)dim.getHeight());
        } else {
          size = new Rectangle(getRowX(row, depth), 0, preferredSize.width, preferredSize.height);
        }
        return size;
      }
      return null;
    }
  }

  /**
   *
   * @param currentWidth Width of the scroll pane currently
   * @param sizeX The row indentation based on depth of the node
   * @param textArea The text area that has the text we want to display
   * @param icon The icon in the node
   * @param textWrap If the text is wrapped or not
   * @return The dimension to best resize to.
   */
  private Dimension getTextDimensions(int currentWidth, int sizeX, JTextArea textArea, Icon icon,
                                      boolean textWrap) {
    FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
    String text = textArea.getText();

    int lineCutOffset = SwingUtilities.computeStringWidth(fm, "-"); //prevents line cut off

    int lines = 0;
    int lastSpace = 0;
    int x = 0;
    int lineWidth = 0;
    int y = 0;
    int newCurrWidth;
    int iconWidth = 0;

    if (icon != null) {
      iconWidth = icon.getIconWidth();
      newCurrWidth = currentWidth - sizeX - iconWidth- 3 - iconInsets;
    } else {
      newCurrWidth = currentWidth - sizeX - 3 - iconInsets;
    }

    if (!textWrap) {
      int largestWidth = 0;
      for (y=0; y < text.length(); y++) {
        lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, y)) + lineCutOffset;
        if((newCurrWidth < lineWidth) && (y - x == 1)){ //the letter we are one is too large for width
          lines++;
          x=y;
          continue;
        }
        char space = text.charAt(y);
        if (space == '\n') {
          x = y + 1;
          y++;
          lines++;
          continue;
        }
        if (lineWidth > largestWidth) {
          largestWidth = lineWidth;
        }
      }
      lines++;
      if (lines < 1) {
        lines = 1;
      }
      int height = (fm.getHeight() * lines) + addHeight;
      largestWidth = largestWidth + sizeX + iconWidth + 3;
      if (largestWidth < currentWidth) {
        largestWidth = currentWidth;
      }
      return new Dimension(largestWidth, height);

    } else { //text wrap is true
      for (y=0; y < text.length(); y++) {
        lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, y)) + lineCutOffset;
        if((newCurrWidth < lineWidth) && (y - x == 1)){ //the letter we are one is too large for width
          lines++;
          x=y;
          continue;
        }
        char space = text.charAt(y);
        if ((space == ' ') || (space == '\t')) {
          lastSpace = y;
        } else if (space == '\n') {
          x = y + 1;
          y++;
          lines++;
          continue;
        }
        if (lineWidth == newCurrWidth) {
          if ((lastSpace > x) && (lastSpace < y)) {
            x = lastSpace;
            y = lastSpace;
          } else {
            x = y;
          }
          lines++;
        } else if (lineWidth > newCurrWidth) {
          int z = y;
          for (; z > 0; z--) {
            lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, z)) +
                lineCutOffset;
            if((newCurrWidth < lineWidth) && (y -x == 0)){ //the letter we are one is too large for width
              lines++;
              x=z;
              y=z;
              break;
            }
            if (lineWidth < newCurrWidth) {
              if ((lastSpace > x) && (lastSpace < y)) {
                x = lastSpace;
                y = lastSpace;
                lines++;
                break;
              } else {
                x = z;
                y = z;
                lines++;
                break;
              }
            }
          }
        }
      }
      if ((x != 0) && (x < y)) { //still some left
        lines++;
      }
    }
    if (lines < 1) {
      lines = 1;
    }
    int height = (fm.getHeight() * lines) + addHeight;

    return new Dimension(currentWidth - sizeX - 3, height);
  }

}
