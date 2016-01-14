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

import com.intellij.ui.JBColor;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The renderer for the project selector, it acts as a gateway for rendering all nodes and for handling mouse events.
 * It creates a single instance of each rendered node and initializes it with current state when necessary.
 */
class ProjectSelectorRenderer implements TreeCellRenderer, MouseListener, MouseMotionListener {

  private static final Cursor NORMAL_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
  private static final Color ERROR_COLOR = JBColor.RED;

  private final ScheduledExecutorService myLoadingAnimationScheduler = ConcurrencyUtil.newSingleScheduledThreadExecutor("Animations");

  private ScheduledFuture<?> myTicker;
  private JTree myTree;
  private DefaultTreeCellRenderer myDefaultRenderer = new DefaultTreeCellRenderer();
  private ProjectSelectorGoogleLogin myProjectSelectorGoogleLogin = new ProjectSelectorGoogleLogin();
  private ProjectSelectorNewProjectItem myProjectSelectorNewProjectItem;
  private ProjectSelectorItem myProjectSelectorItem;
  private ProjectSelectorCredentialedUser myProjectSelectorCredentialedUser = new ProjectSelectorCredentialedUser();
  private ProjectSelectorLoadingItem myProjectSelectorLoadingItem;
  private ProjectSelectorErrorItem mySelectorErrorItem;
  private DefaultMutableTreeNode myLastHoveredNode;

  public ProjectSelectorRenderer(@NotNull JTree tree) {
    myTree = tree;
    Color backgroundNonSelectionColor = myDefaultRenderer.getBackgroundNonSelectionColor();
    Color textNonSelectionColor = myDefaultRenderer.getTextNonSelectionColor();
    myProjectSelectorItem = new ProjectSelectorItem(backgroundNonSelectionColor,
                                                    myDefaultRenderer.getTextSelectionColor(), textNonSelectionColor);
    myProjectSelectorLoadingItem = new ProjectSelectorLoadingItem(backgroundNonSelectionColor, textNonSelectionColor);
    myProjectSelectorNewProjectItem = new ProjectSelectorNewProjectItem(myTree);
    mySelectorErrorItem = new ProjectSelectorErrorItem(ERROR_COLOR);
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                boolean leaf, int row, boolean hasFocus) {
    Component returnValue = null;
    if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
      returnValue = getComponentForNode(value, selected);
    }
    if (returnValue == null) {
      returnValue = myDefaultRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
    return returnValue;
  }

  public int getMaximumWidth() {
    return myProjectSelectorNewProjectItem.getPreferredSize().width;
  }

  // This method causes all loading nodes to repaint (for animation purposes)
  // If there are no further loading nodes to paint, it turns off the ticker.
  private void repaintLoadingNodes() {
    boolean hasLoadingNode = false;
    DefaultTreeModel model = (DefaultTreeModel)myTree.getModel();
    DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)model.getRoot();
    for(int index = 0; index < rootNode.getChildCount(); index++ ) {
      GoogleUserModelItem userModelItem = (GoogleUserModelItem)rootNode.getChildAt(index);
      if (userModelItem.isSynchronizing() &&
          userModelItem.getChildCount() == 1 && userModelItem.getChildAt(0) instanceof ElysiumLoadingModelItem) {
        TreePath path = new TreePath(model.getPathToRoot(userModelItem.getChildAt(0)));
        Rectangle rect = myTree.getPathBounds(path);
        if (rect != null) {
          myTree.repaint(rect);
          hasLoadingNode = true;
        }
      }
    }

    if (!hasLoadingNode) {
      myTicker.cancel(false);
      myTicker = null;
    }
  }

  @Nullable
  private Component getComponentForNode(Object userObject, boolean selected) {
    if (userObject instanceof GoogleSignOnModelItem) {
      return myProjectSelectorGoogleLogin;
    }
    else if (userObject instanceof ElysiumNewProjectModelItem) {
      return myProjectSelectorNewProjectItem;
    }
    else if (userObject instanceof  ElysiumLoadingModelItem) {
      if (myTicker == null) {
        myTicker = myLoadingAnimationScheduler.scheduleWithFixedDelay(new Runnable() {
          @Override
          public void run() {
            repaintLoadingNodes();
          }
        }, 100, 100, TimeUnit.MILLISECONDS);
      }
      myProjectSelectorLoadingItem.snap();
      return myProjectSelectorLoadingItem;
    }
    else if (userObject instanceof  ElysiumErrorModelItem) {
      mySelectorErrorItem.setText( ((ElysiumErrorModelItem)userObject).getErrorMessage());
      return mySelectorErrorItem;
    }
    else if (userObject instanceof ElysiumProjectModelItem) {
      myProjectSelectorItem
        .initialize(((ElysiumProjectModelItem)userObject).getDescription(),
                    ((ElysiumProjectModelItem)userObject).getProjectId(), selected,
                    myLastHoveredNode == userObject);

      return myProjectSelectorItem;
    }
    else if (userObject instanceof GoogleUserModelItem) {
      GoogleUserModelItem userModelItem = (GoogleUserModelItem)userObject;
      myProjectSelectorCredentialedUser.initialize(userModelItem.getImage(), userModelItem.getName(), userModelItem.getEmail());
      return myProjectSelectorCredentialedUser;
    }
    return null;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
    if (path != null && path.getLastPathComponent() instanceof GoogleUserModelItem) {
      if (myTree.isCollapsed(path)) {
        myTree.expandPath(path);
      }
      else {
        myTree.collapsePath(path);
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    Component component = getComponentFromXY(e.getX(), e.getY(), false);
    if (component instanceof MouseListener) {
      ((MouseListener)component).mousePressed(
        new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
                       getXTranslation(e.getX(), e.getY()), getYTranslation(e.getX(), e.getY()),
                       e.getClickCount(), e.isPopupTrigger(), e.getButton()));
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
    boolean mouseMovedHandled = false;
    TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
    if (path != null && path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode newHoveredNode = (DefaultMutableTreeNode)path.getLastPathComponent();
      if (newHoveredNode != myLastHoveredNode) {
        Rectangle rect;
        if (myLastHoveredNode != null) {
          rect = myTree.getPathBounds(new TreePath(myLastHoveredNode.getPath()));
          if (rect != null) {
            myTree.repaint(rect);
          }
        }
        myLastHoveredNode = newHoveredNode;
        rect = myTree.getPathBounds(new TreePath(myLastHoveredNode.getPath()));
        if (rect != null) {
          myTree.repaint(rect);
        }
      }
      Component component = getComponentForNode(newHoveredNode, false);

      if (component instanceof MouseMotionListener) {
        ((MouseMotionListener)component).mouseMoved(
          new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(),
                         getXTranslation(e.getX(), e.getY()), getYTranslation(e.getX(), e.getY()),
                         e.getClickCount(), e.isPopupTrigger(), e.getButton()));
        mouseMovedHandled = true;
      }
    }
    if (!mouseMovedHandled) {
      myTree.setCursor(NORMAL_CURSOR);
    }
  }

  @Nullable
  private Component getComponentFromXY(int x, int y, boolean selected) {
    TreePath path = myTree.getPathForLocation(x, y);
    if (path != null) {
      Object node = path.getLastPathComponent();
      if (node instanceof DefaultMutableTreeNode) {
        return getComponentForNode(node, selected);
      }
    }
    return null;
  }

  private int getXTranslation(int x, int y) {
    TreePath path = myTree.getPathForLocation(x, y);
    Rectangle nodeBounds = myTree.getPathBounds(path);
    return x - (nodeBounds != null ? nodeBounds.x : 0);
  }

  private int getYTranslation(int x, int y) {
    TreePath path = myTree.getPathForLocation(x, y);
    Rectangle nodeBounds = myTree.getPathBounds(path);
    return y - (nodeBounds != null ? nodeBounds.y : 0);
  }
}
