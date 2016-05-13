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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.cloud.tools.intellij.login.CredentialedUser;
import com.google.cloud.tools.intellij.login.IGoogleLoginCompletedCallback;
import com.google.cloud.tools.intellij.login.IntellijGoogleLoginService;
import com.google.cloud.tools.intellij.login.Services;
import com.google.cloud.tools.intellij.login.ui.GoogleLoginEmptyPanel;
import com.google.cloud.tools.intellij.ui.CustomizableComboBox;
import com.google.cloud.tools.intellij.ui.CustomizableComboBoxPopup;
import com.google.cloud.tools.intellij.ui.GoogleCloudToolsIcons;

import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * ProjectSelector allows the user to select an Elysium project id. It calls into {@link
 * IntellijGoogleLoginService} to get the set of credentialed users and then into elysium to get the
 * set of projects. The result is displayed in a tree view organized by google login.
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

  private final DefaultMutableTreeNode modelRoot;
  private final SelectorTreeModel treeModel;
  private final boolean queryOnExpand;
  private JBPopup popup;
  private PopupPanel popupPanel;

  public ProjectSelector() {
    this(false);
  }

  /**
   * Initialize the project selector.
   */
  public ProjectSelector(final boolean queryOnExpand) {
    this.queryOnExpand = queryOnExpand;
    modelRoot = new DefaultMutableTreeNode("root");
    treeModel = new SelectorTreeModel(modelRoot);

    // synchronize selection between the treemodel and current text.
    treeModel.addTreeModelListener(new TreeModelListener() {
      @Override
      public void treeNodesChanged(TreeModelEvent event) {
      }

      @Override
      public void treeNodesInserted(TreeModelEvent event) {
      }

      @Override
      public void treeNodesRemoved(TreeModelEvent event) {
      }

      @Override
      public void treeStructureChanged(TreeModelEvent event) {
        if (!Strings.isNullOrEmpty(getText())
            && popup != null && !popup.isDisposed() && popupPanel != null
            && event.getTreePath() != null
            && event.getTreePath().getLastPathComponent() instanceof GoogleUserModelItem) {
          GoogleUserModelItem userItem = (GoogleUserModelItem) event.getTreePath()
              .getLastPathComponent();
          for (int index = 0; index < userItem.getChildCount(); index++) {
            DefaultMutableTreeNode loadedItem = (DefaultMutableTreeNode) userItem.getChildAt(index);
            if (loadedItem instanceof ElysiumProjectModelItem
                && getText().equals(((ElysiumProjectModelItem) loadedItem).getProjectId())) {
              popupPanel.tree.setSelectionPath(new TreePath(loadedItem.getPath()));
            }
          }
        }
      }
    });

    // When the project selector becomes visible, we synchronize and call elysium.
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent event) {
        synchronize(false);
        if (EMPTY_MARKER.equals(getText())) {
          setText(EMPTY_VALUE);
        }
      }

      @Override
      public void componentHidden(ComponentEvent event) {
        if (EMPTY_VALUE.equals(getText())) {
          setText(EMPTY_MARKER);
        }
      }
    });

    getTextField().setCursor(Cursor.getDefaultCursor());
    getTextField().getEmptyText().setText("Enter a cloud project ID...");

    // Instead of doing an initial synchronize, we wait until the ui hierarchy
    // is about to be shown.  Then we only synchronize if we are visible.
    // This will prevent superfluous calls into Elysium when the user uses
    // the play services activity wizard, but never selects enable cloudsave.
    // In cases outside of the wizard (such as deploy), we will be visible,
    // so the call to elysium will happen immediately when the hierarchy is shown.
    addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(HierarchyEvent event) {
        if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
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

  public void addModelListener(TreeModelListener listener) {
    treeModel.addTreeModelListener(listener);
  }

  public void removeModelListener(TreeModelListener listener) {
    treeModel.removeTreeModelListener(listener);
  }

  /**
   * Returns the selected credentialed user for the project id represented by {@link #getText()}.
   * <p/>
   * Note: if the ProjectSelector is created with queryOnExpand, this value could be {@code null}
   * even if {@link #getText()} represents a valid project because the user has not expanded the
   * owning {@link IntellijGoogleLoginService}.
   */
  @Nullable
  public CredentialedUser getSelectedUser() {
    if (Strings.isNullOrEmpty(getText())) {
      return null;
    }

    // Look for the selected text in the model, which will give us the login.
    for (int i = 0; i < modelRoot.getChildCount(); i++) {
      TreeNode node = modelRoot.getChildAt(i);
      if (node instanceof GoogleUserModelItem) {
        for (int j = 0; j < node.getChildCount(); j++) {
          TreeNode projectNode = node.getChildAt(j);
          if (projectNode instanceof ElysiumProjectModelItem
              && getText().equals(((ElysiumProjectModelItem) projectNode).getProjectId())) {
            return ((GoogleUserModelItem) node).getCredentialedUser();
          }
        }
      }
    }

    return null;
  }

  /**
   * Returns the selected project's description.
   * <p/>
   * This has the same limitations as {@link #getSelectedUser()}  in that it may be null even if
   * getText represents a valid ID if queryOnExpand is true.
   */
  @Nullable
  public String getProjectDescription() {
    ElysiumProjectModelItem modelItem = getCurrentModelItem();
    return modelItem != null ? modelItem.getDescription() : null;
  }

  /**
   * Returns the selected project's numeric code.
   * <p/>
   * This has the same limitations as {@link #getSelectedUser()}  in that it may be null even if
   * getText represents a valid ID if queryOnExpand is true.
   */
  @Nullable
  public Long getProjectNumber() {
    ElysiumProjectModelItem modelItem = getCurrentModelItem();
    return modelItem != null ? modelItem.getNumber() : null;
  }

  @Nullable
  private ElysiumProjectModelItem getCurrentModelItem() {
    if (Strings.isNullOrEmpty(getText())) {
      return null;
    }

    // Look for the selected text in the model, which will give us the login.
    for (int i = 0; i < modelRoot.getChildCount(); i++) {
      TreeNode userNode = modelRoot.getChildAt(i);
      if (userNode instanceof GoogleUserModelItem) {
        for (int j = 0; j < userNode.getChildCount(); j++) {
          TreeNode projectNode = userNode.getChildAt(j);
          if (projectNode instanceof ElysiumProjectModelItem
              && getText().equals(((ElysiumProjectModelItem) projectNode).getProjectId())) {
            return ((ElysiumProjectModelItem) projectNode);
          }
        }
      }
    }

    return null;
  }

  @Override
  protected int getPreferredPopupHeight() {
    return !needsToSignIn() ? PREFERRED_HEIGHT
        : BaseGoogleLoginUi.PREFERRED_HEIGHT + POPUP_HEIGHTFRAMESIZE;
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
    for (int index = 0; index < modelRoot.getChildCount(); index++) {
      TreeNode node = modelRoot.getChildAt(index);
      if (node instanceof GoogleUserModelItem) {
        String currentNodeEmail = ((GoogleUserModelItem) node).getCredentialedUser().getEmail();
        if (!Strings.isNullOrEmpty(currentNodeEmail) && currentNodeEmail.equals(user.getEmail())) {
          return (GoogleUserModelItem) node;
        }
      }
    }

    GoogleUserModelItem newUser = new GoogleUserModelItem(user, treeModel);
    treeModel.insertNodeInto(newUser, modelRoot, modelRoot.getChildCount());
    return newUser;
  }

  private static boolean needsToSignIn() {
    Map<String, CredentialedUser> users = Services.getLoginService().getAllUsers();

    return users.isEmpty();
  }

  @SuppressWarnings("AssignmentToForLoopParameter")
  private void synchronize(boolean forceUpdate) {
    // First, clear any users that went away.

    // Put all users in a set for fast access.
    Set<String> emailUsers = new HashSet<String>();
    if (!needsToSignIn()) {
      for (CredentialedUser user : Services.getLoginService().getAllUsers().values()) {
        emailUsers.add(user.getEmail());
      }
    }
    for (int index = 0; index < modelRoot.getChildCount(); ) {
      TreeNode node = modelRoot.getChildAt(index);
      if (node instanceof GoogleUserModelItem) {
        CredentialedUser user = ((GoogleUserModelItem) node).getCredentialedUser();
        // If the user isn't valid anymore, remove the corresponding node..
        if (user == null || !emailUsers.contains(user.getEmail())) {
          treeModel.removeNodeFromParent((GoogleUserModelItem) node);
          continue;
        }
      } else {
        treeModel.removeNodeFromParent((MutableTreeNode) node);
        continue;
      }
      index++;
    }

    // Now add users that haven't been added
    if (!needsToSignIn()) {
      GoogleUserModelItem node = getNodeForUser(Services.getLoginService().getActiveUser());
      if (node != null) {
        if (forceUpdate) {
          node.setNeedsSynchronizing();
        }
        node.synchronize();
      }

      for (CredentialedUser user : Services.getLoginService().getAllUsers().values()) {
        if (user != Services.getLoginService().getActiveUser()) {
          node = getNodeForUser(user);
          if (node != null) {
            if (forceUpdate) {
              node.setNeedsSynchronizing();
            }
            if (!queryOnExpand
                || popupPanel != null
                && popupPanel.tree.isExpanded(new TreePath(node.getPath()))) {
              node.synchronize();
            }
          }
        }
      }
    } else {
      treeModel.insertNodeInto(new GoogleSignOnModelItem(), modelRoot, 0);
    }
  }

  @Override
  public void showPopup(RelativePoint showTarget) {
    if (popup == null || popup.isDisposed()) {
      popupPanel = new PopupPanel();

      popupPanel.initializeContent(getText());
      ComponentPopupBuilder popup = JBPopupFactory.getInstance()
          .createComponentPopupBuilder(popupPanel, popupPanel.getInitialFocus());
      this.popup = popup.createPopup();
    }
    if (!popup.isVisible()) {
      popup.show(showTarget);
    }
  }

  private class PopupPanel extends GoogleLoginEmptyPanel {

    private JTree tree;

    @Nullable
    private TreePath find(DefaultMutableTreeNode root, String str) {
      @SuppressWarnings("unchecked") Enumeration<DefaultMutableTreeNode> nodes = root
          .depthFirstEnumeration();
      while (nodes.hasMoreElements()) {
        DefaultMutableTreeNode node = nodes.nextElement();
        if (node instanceof ElysiumProjectModelItem
            && str.equalsIgnoreCase(((ElysiumProjectModelItem) node).getProjectId())) {
          return new TreePath(node.getPath());
        }
      }
      return null;
    }

    public JComponent getInitialFocus() {
      return tree;
    }

    public void initializeContent(String selectedProjectId) {
      tree = new Tree(treeModel);
      tree.setRowHeight(0);

      if (!Strings.isNullOrEmpty(selectedProjectId)) {
        TreePath path = find(modelRoot, selectedProjectId);
        if (path != null) {
          tree.setSelectionPath(path);
        }
      }
      tree.setRootVisible(false);
      tree.setOpaque(false);
      tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      ProjectSelectorRenderer renderer = new ProjectSelectorRenderer(tree);
      tree.addMouseListener(renderer);
      tree.addMouseMotionListener(renderer);
      tree.setCellRenderer(renderer);
      this.getContentPane()
          .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      this.getContentPane().setViewportView(tree);
      tree.addTreeSelectionListener(new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent event) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree
              .getLastSelectedPathComponent();
          if (node != null) {
            if (node instanceof ElysiumProjectModelItem) {
              if (Strings.isNullOrEmpty(ProjectSelector.this.getText())
                  || !ProjectSelector.this.getText()
                  .equals(((ElysiumProjectModelItem) node).getProjectId())) {
                ProjectSelector.this.setText(((ElysiumProjectModelItem) node).getProjectId());
                SwingUtilities.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                    ProjectSelector.this.hidePopup();
                  }
                });
              }
            } else {
              tree.clearSelection();
            }
          }
        }
      });

      if (queryOnExpand) {
        tree.addTreeExpansionListener(new TreeExpansionListener() {
          @Override
          public void treeExpanded(TreeExpansionEvent event) {
            TreePath expandedPath = event.getPath();
            if (expandedPath != null && expandedPath
                .getLastPathComponent() instanceof GoogleUserModelItem) {
              ((GoogleUserModelItem) expandedPath.getLastPathComponent()).synchronize();
            }
          }

          @Override
          public void treeCollapsed(TreeExpansionEvent event) {
          }
        });
      }

      for (int i = 0; i < tree.getRowCount(); i++) {
        tree.expandRow(i);
        TreePath path = tree.getPathForRow(i);
        if (path.getLastPathComponent() instanceof GoogleUserModelItem) {
          break; // Remove this to expand all rows on show.
        }
      }

      tree.requestFocusInWindow();
      Insets thisInsets = this.getInsets();
      Insets contentInset = this.getContentPane().getInsets();
      Insets treeInset = tree.getInsets();

      int preferredWidth = renderer.getMaximumWidth()
          + UIUtil.getTreeLeftChildIndent() * 2
          + UIUtil.getTreeExpandedIcon().getIconWidth() * 2
          + UIUtil.getScrollBarWidth()
          + (thisInsets != null ? (thisInsets.left + thisInsets.right) : 0)
          + (contentInset != null ? (contentInset.left + contentInset.right) : 0)
          + (treeInset != null ? (treeInset.left + treeInset.right) : 0);

      preferredWidth = Math.max(preferredWidth, ProjectSelector.this.getWidth());
      this.setPreferredSize(new Dimension(Math.max(BaseGoogleLoginUi.MIN_WIDTH, preferredWidth),
          getPreferredPopupHeight()));

      getBottomPane().setLayout(new BorderLayout());

      if (!needsToSignIn()) {
        JButton synchronizeButton = new JButton();
        synchronizeButton.setIcon(GoogleCloudToolsIcons.REFRESH);
        synchronizeButton.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent event) {
            if (!needsToSignIn()) {
              synchronize(true);
            }
          }
        });

        getBottomPane().add(synchronizeButton, BorderLayout.EAST);
      }

      if (treeModel.isModelNeedsRefresh()) {
        treeModel.setModelNeedsRefresh(false);
        synchronize(true);
      }
    }

    @Override
    protected void doLogin() {
      Services.getLoginService().logIn(null, new IGoogleLoginCompletedCallback() {

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
      popup.closeOk(null);
    }
  }

  @Override
  public boolean isPopupVisible() {
    return popup != null && !popup.isDisposed() && popup.isVisible();
  }

  static class SelectorTreeModel extends DefaultTreeModel {

    private boolean modelNeedsRefresh;

    public SelectorTreeModel(TreeNode root) {
      super(root);
    }

    public boolean isModelNeedsRefresh() {
      return modelNeedsRefresh;
    }

    public void setModelNeedsRefresh(boolean modelNeedsRefresh) {
      this.modelNeedsRefresh = modelNeedsRefresh;
    }
  }
}
