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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.gct.idea.ui.CustomizableComboBox;
import com.google.gct.idea.ui.CustomizableComboBoxPopup;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.google.gct.login.IGoogleLoginCompletedCallback;
import com.google.gct.login.ui.GoogleLoginEmptyPanel;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;
import icons.GoogleCloudToolsIcons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ProjectSelector allows the user to select an Elysium project id.
 * It calls into {@link GoogleLogin} to get the set of credentialed users and then into elysium to get the set of projects.
 * The result is displayed in a tree view organized by google login.
 */
public class ProjectSelector extends CustomizableComboBox implements CustomizableComboBoxPopup {
  // An empty marker is used because the template engine validates even
  // when the control is not visible (e.g. cloudsave isn't chosen).
  // Changing the template engine isn't possible here without a regression
  // of other templates -- as other templates use invisible
  // controls to hold state that is used by freemarker.
  private static final String EMPTY_MARKER = "(empty)";
  private static final String EMPTY_VALUE = "";
  private static final int PREFERRED_HEIGHT = 240;
  private static final int POPUP_HEIGHTFRAMESIZE = 50;
  public static final int MIN_WIDTH = 450;

  private final DefaultMutableTreeNode myModelRoot;
  private final DefaultTreeModel myTreeModel;
  private final boolean myQueryOnExpand;
  private JBPopup myJBPopup;
  private PopupPanel myPopupPanel;

  public ProjectSelector() {
    this(false);
  }

  public ProjectSelector(final boolean queryOnExpand) {
    myQueryOnExpand = queryOnExpand;
    myModelRoot = new DefaultMutableTreeNode("root");
    myTreeModel = new DefaultTreeModel(myModelRoot);

    // synchronize selection between the treemodel and current text.
    myTreeModel.addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent e) {
      }

      @Override
      public void treeNodesInserted(TreeModelEvent e) {
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent e) {
      }

      @Override
      public void treeStructureChanged(TreeModelEvent e) {
        if (!Strings.isNullOrEmpty(getText()) &&
            myJBPopup != null && !myJBPopup.isDisposed() && myPopupPanel != null &&
            e.getTreePath() != null &&
            e.getTreePath().getLastPathComponent() instanceof GoogleUserModelItem) {
          GoogleUserModelItem userItem = (GoogleUserModelItem) e.getTreePath().getLastPathComponent();
          for (int index = 0; index < userItem.getChildCount(); index++) {
            DefaultMutableTreeNode loadedItem = (DefaultMutableTreeNode) userItem.getChildAt(index);
            if (loadedItem instanceof ElysiumProjectModelItem &&
                getText().equals(((ElysiumProjectModelItem) loadedItem).getProjectId())) {
              myPopupPanel.myJTree.setSelectionPath(new TreePath(loadedItem.getPath()));
            }
          }
        }
      }
    });

    // When the project selector becomes visible, we synchronize and call elysium.
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        synchronize(false);
        if (EMPTY_MARKER.equals(getText())) {
          setText(EMPTY_VALUE);
        }
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        if (EMPTY_VALUE.equals(getText())) {
          setText(EMPTY_MARKER);
        }
      }
    });

    getTextField().setCursor(Cursor.getDefaultCursor());
    getTextField().getEmptyText().setText("Please select a project...");

    // Instead of doing an initial synchronize, we wait until the ui hierarchy
    // is about to be shown.  Then we only synchronize if we are visible.
    // This will prevent superfluous calls into Elysium when the user uses
    // the play services activity wizard, but never selects enable cloudsave.
    // In cases outside of the wizard (such as deploy), we will be visible,
    // so the call to elysium will happen immediately when the hierarchy is shown.
    addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              if (ProjectSelector.this.isVisible()) {
                synchronize(false);
              }
            }
          });
        }
      }
    });
  }

  /**
   * Returns the selected credentialed user for the project id represented by getText().
   * Note that if the ProjectSelector is created with queryOnExpand, this value could be {@code null} even
   * if {@link #getText()} represents a valid project because the user has not expanded the owning {@link GoogleLogin}.
   */
  @Nullable
  public CredentialedUser getSelectedUser() {
    if (Strings.isNullOrEmpty(getText())) {
      return null;
    }

    // Look for the selected text in the model, which will give us the login.
    for (int i = 0; i < myModelRoot.getChildCount(); i++) {
      TreeNode node = myModelRoot.getChildAt(i);
      if (node instanceof GoogleUserModelItem) {
        for (int j = 0; j < node.getChildCount(); j++) {
          TreeNode projectNode = node.getChildAt(j);
          if (projectNode instanceof ElysiumProjectModelItem &&
              getText().equals(((ElysiumProjectModelItem) projectNode).getProjectId())) {
            return ((GoogleUserModelItem) node).getCredentialedUser();
          }
        }
      }
    }

    return null;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return !needsToSignIn() ? PREFERRED_HEIGHT : ProjectSelectorGoogleLogin.PREFERRED_HEIGHT + POPUP_HEIGHTFRAMESIZE;
  }

  @Override
  protected CustomizableComboBoxPopup getPopup() {
    return this;
  }

  // Demand creates a model item node for a given user.  Caches the result.
  @Nullable
  private GoogleUserModelItem getNodeForUser(@Nullable CredentialedUser user) {
    if (user == null) {
      return null;
    }
    for (int index = 0; index < myModelRoot.getChildCount(); index++) {
      TreeNode node = myModelRoot.getChildAt(index);
      if (node instanceof GoogleUserModelItem) {
        String currentNodeEmail = ((GoogleUserModelItem) node).getCredentialedUser().getEmail();
        if (!Strings.isNullOrEmpty(currentNodeEmail) && currentNodeEmail.equals(user.getEmail())) {
          return (GoogleUserModelItem) node;
        }
      }
    }

    GoogleUserModelItem newUser = new GoogleUserModelItem(user, myTreeModel);
    myTreeModel.insertNodeInto(newUser, myModelRoot, myModelRoot.getChildCount());
    return newUser;
  }

  private static boolean needsToSignIn() {
    Map<String, CredentialedUser> users = GoogleLogin.getInstance().getAllUsers();

    return users == null || users.isEmpty();
  }

  private void synchronize(boolean forceUpdate) {
    // First, clear any users that went away.

    // Put all users in a set for fast access.
    Set<String> emailUsers = new HashSet<String>();
    if (!needsToSignIn()) {
      for (CredentialedUser user : GoogleLogin.getInstance().getAllUsers().values()) {
        emailUsers.add(user.getEmail());
      }
    }
    for (int index = 0; index < myModelRoot.getChildCount(); ) {
      TreeNode node = myModelRoot.getChildAt(index);
      if (node instanceof GoogleUserModelItem) {
        CredentialedUser user = ((GoogleUserModelItem) node).getCredentialedUser();
        // If the user isn't valid anymore, remove the corresponding node..
        if (user == null || !emailUsers.contains(user.getEmail())) {
          myTreeModel.removeNodeFromParent((GoogleUserModelItem) node);
          continue;
        }
      }
      else {
        myTreeModel.removeNodeFromParent((MutableTreeNode) node);
        continue;
      }
      index++;
    }

    // Now add users that haven't been added
    if (!needsToSignIn()) {
      GoogleUserModelItem node = getNodeForUser(GoogleLogin.getInstance().getActiveUser());
      if (node != null) {
        if (forceUpdate) {
          node.setNeedsSynchronizing();
        }
        node.synchronize();
      }

      for (CredentialedUser user : GoogleLogin.getInstance().getAllUsers().values()) {
        if (user != GoogleLogin.getInstance().getActiveUser()) {
          node = getNodeForUser(user);
          if (node != null) {
            if (forceUpdate) {
              node.setNeedsSynchronizing();
            }
            if (!myQueryOnExpand ||
                myPopupPanel != null && myPopupPanel.myJTree.isExpanded(new TreePath(node.getPath()))) {
              node.synchronize();
            }
          }
        }
      }
    }
    else {
      myTreeModel.insertNodeInto(new GoogleSignOnModelItem(), myModelRoot, 0);
    }
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (myJBPopup == null || myJBPopup.isDisposed()) {
      myPopupPanel = new PopupPanel();

      myPopupPanel.initializeContent(getText());
      ComponentPopupBuilder popup = JBPopupFactory.getInstance().
        createComponentPopupBuilder(myPopupPanel, myPopupPanel.getInitialFocus());
      myJBPopup = popup.createPopup();
    }
    if (!myJBPopup.isVisible()) {
      myJBPopup.show(showTarget);
    }
  }

  private class PopupPanel extends GoogleLoginEmptyPanel {
    private JTree myJTree;

    @Nullable
    private TreePath find(DefaultMutableTreeNode root, String s) {
      @SuppressWarnings("unchecked") Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
      while (e.hasMoreElements()) {
        DefaultMutableTreeNode node = e.nextElement();
        if (node instanceof ElysiumProjectModelItem &&
            s.equalsIgnoreCase(((ElysiumProjectModelItem) node).getProjectId())) {
          return new TreePath(node.getPath());
        }
      }
      return null;
    }

    public JComponent getInitialFocus() {
      return myJTree;
    }

    public void initializeContent(String selectedProjectId) {
      myJTree = new Tree(myTreeModel);
      myJTree.setRowHeight(0);

      if (!Strings.isNullOrEmpty(selectedProjectId)) {
        TreePath path = find(myModelRoot, selectedProjectId);
        if (path != null) {
          myJTree.setSelectionPath(path);
        }
      }
      myJTree.setRootVisible(false);
      myJTree.setOpaque(false);
      myJTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      ProjectSelectorRenderer renderer = new ProjectSelectorRenderer(myJTree);
      myJTree.addMouseListener(renderer);
      myJTree.addMouseMotionListener(renderer);
      myJTree.setCellRenderer(renderer);
      this.getContentPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      this.getContentPane().setViewportView(myJTree);
      myJTree.addTreeSelectionListener(new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) myJTree.getLastSelectedPathComponent();
          if (node != null) {
            if (node instanceof ElysiumProjectModelItem) {
              if (Strings.isNullOrEmpty(ProjectSelector.this.getText()) ||
                  !ProjectSelector.this.getText().equals(((ElysiumProjectModelItem) node).getProjectId())) {
                ProjectSelector.this.setText(((ElysiumProjectModelItem) node).getProjectId());
                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    ProjectSelector.this.hidePopup();
                  }
                });
              }
            }
            else {
              myJTree.clearSelection();
            }
          }
        }
      });

      if (myQueryOnExpand) {
        myJTree.addTreeExpansionListener(new TreeExpansionListener() {
          @Override
          public void treeExpanded(TreeExpansionEvent event) {
            TreePath expandedPath = event.getPath();
            if (expandedPath != null && expandedPath.getLastPathComponent() instanceof GoogleUserModelItem) {
              ((GoogleUserModelItem) expandedPath.getLastPathComponent()).synchronize();
            }
          }

          @Override
          public void treeCollapsed(TreeExpansionEvent event) {
          }
        });
      }

      for (int i = 0; i < myJTree.getRowCount(); i++) {
        myJTree.expandRow(i);
        TreePath path = myJTree.getPathForRow(i);
        if (path.getLastPathComponent() instanceof GoogleUserModelItem) {
          break; // Remove this to expand all rows on show.
        }
      }

      myJTree.requestFocusInWindow();
      Insets thisInsets = this.getInsets();
      Insets contentInset = this.getContentPane().getInsets();
      Insets treeInset = myJTree.getInsets();

      int preferredWidth = renderer.getMaximumWidth() +
                           UIUtil.getTreeLeftChildIndent() * 2 +
                           UIUtil.getTreeExpandedIcon().getIconWidth() * 2 +
                           UIUtil.getScrollBarWidth() +
                           (thisInsets != null ? (thisInsets.left + thisInsets.right) : 0) +
                           (contentInset != null ? (contentInset.left + contentInset.right) : 0) +
                           (treeInset != null ? (treeInset.left + treeInset.right) : 0);

      this.setPreferredSize(new Dimension(Math.max(MIN_WIDTH, preferredWidth), getPreferredPopupHeight()));

      getBottomPane().setLayout(new BorderLayout());
      JButton synchronizeButton = new JButton();
      synchronizeButton.setIcon(GoogleCloudToolsIcons.REFRESH);
      synchronizeButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (!needsToSignIn()) {
            synchronize(true);
          }
        }
      });

      getBottomPane().add(synchronizeButton, BorderLayout.EAST);
    }

    @Override
    protected void doLogin() {
      GoogleLogin.getInstance().logIn(null, new IGoogleLoginCompletedCallback() {

        @Override
        public void onLoginCompleted() {
          synchronize(true);
        }
      });
    }
  }

  @Override
  public void hidePopup() {
    if (isPopupVisible()) {
      myJBPopup.closeOk(null);
    }
  }

  @Override
  public boolean isPopupVisible() {
    return myJBPopup != null && !myJBPopup.isDisposed() && myJBPopup.isVisible();
  }
}
